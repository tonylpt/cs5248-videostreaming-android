package com.cs5248.android.service;

import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Pair;

import com.cs5248.android.Config;
import com.cs5248.android.model.Video;
import com.cs5248.android.model.VideoSegment;
import com.cs5248.android.service.job.DownloadJob;
import com.cs5248.android.service.job.FileRemoveJob;
import com.cs5248.android.service.job.MpdDownloadJob;
import com.cs5248.android.service.job.SegmentDownloadJob;
import com.google.android.exoplayer.C;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.dash.mpd.AdaptationSet;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescription;
import com.google.android.exoplayer.dash.mpd.Period;
import com.google.android.exoplayer.dash.mpd.Representation;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import timber.log.Timber;

import static com.cs5248.android.service.StreamingSession.Streamlet.Status.DOWNLOADED;
import static com.cs5248.android.service.StreamingSession.Streamlet.Status.ERROR;
import static com.cs5248.android.service.StreamingSession.Streamlet.Status.PENDING;
import static com.cs5248.android.service.StreamingState.ENDED;
import static com.cs5248.android.service.StreamingState.NOT_STARTED;
import static com.cs5248.android.service.StreamingState.PROGRESSING;
import static com.google.android.exoplayer.dash.mpd.Representation.MultiSegmentRepresentation;

/**
 * @author lpthanh
 */
public abstract class StreamingSession {

    private final StreamingService streamingService;

    private final JobService jobService;

    @Getter
    private final Video video;

    @Getter
    private final File storageDir;

    @Getter
    private VideoSegment currentSegment;

    @Getter
    private volatile StreamingState streamingState;

    @Setter
    private StateChangeListener stateChangeListener;

    /**
     * Stores the last segment ID returned from the server MPD. So that the next MPD only needs to
     * return the new segments.
     */
    private Long lastSegmentId;

    /**
     * The download speed of the last streamlet, in bytes per second.
     */
    private float lastSpeed;

    protected StreamingSession(Context context,
                               StreamingService streamingService,
                               JobService jobService,
                               Video video,
                               File storageDir) {
        // validate
        Objects.requireNonNull(video);
        Objects.requireNonNull(video.getVideoId());
        Objects.requireNonNull(storageDir);

        this.streamingService = streamingService;
        this.jobService = jobService;
        this.video = video;
        this.storageDir = storageDir;

        this.streamingState = NOT_STARTED;
    }

    public final void startStreaming() {
        if (streamingState != NOT_STARTED) {
            Timber.w("Cannot start streaming when state=%s", streamingState);
            return;
        }

        setStreamingState(PROGRESSING);

        // start the first MPD download
        jobService.submitJob(new MpdDownloadJob(this));
    }

    /**
     * Invoked by user to stop the streaming
     */
    public final void endStreaming() {
        if (streamingState != PROGRESSING) {
            Timber.w("Cannot end streaming when state=%s", streamingState);
            return;
        }

        cleanUp();
        streamingEnded();
    }

    /**
     * Mark the end of the streaming session, either by user or because the stream has ended.
     */
    private void streamingEnded() {
        setStreamingState(ENDED);
        currentSegment = null;
    }

    /**
     * Cleans up the storage directory after playback.
     * And remove all pending jobs for streaming.
     */
    private void cleanUp() {
        jobService.removeJobByTag(DownloadJob.STREAMING_JOB_TAG);
        jobService.submitJob(new FileRemoveJob(storageDir, true));
    }

    private void segmentDownloaded(VideoSegment segment) {
        currentSegment = segment;
    }


    private void setStreamingState(StreamingState newState) {
        StreamingState lastState = this.streamingState;
        this.streamingState = newState;
        if (lastState != newState) {
            if (stateChangeListener != null) {
                try {
                    stateChangeListener.stateChanged(streamingState);
                } catch (Exception e) {
                    Timber.e(e, "Strange error");
                }
            }
        }
    }

    public boolean isProgressing() {
        return getStreamingState() == PROGRESSING;
    }

    /**
     * To be called by the job queue.
     * <p>
     * Download the MPD. If this is a live streaming session, schedule the next MPD download.
     * Dispatch job to download the needed segment files.
     */
    public void _jobPerformUpdateMpd() {
        if (!isProgressing()) {
            return;
        }

        Pair<MediaPresentationDescription, Long> mpdResponse = streamingService.getMpd(getVideo(), lastSegmentId);
        if (mpdResponse == null) {
            throw new StreamingException("Unable to download MPD for video", getVideo().getVideoId());
        }

        this.lastSegmentId = mpdResponse.second;

        try {
            Period period = mpdResponse.first.getPeriod(0);
            AdaptationSet adaptationSet = period.adaptationSets.get(0);
            List<Representation> reprs = adaptationSet.representations;

            // ensures we have all three representations
            if (reprs.size() < 3) {
                throw new IllegalStateException("Expected 3 representations in the MPD");
            }

            // get the segments
            MultiSegmentRepresentation oneRepr = (MultiSegmentRepresentation) reprs.get(0);
            int firstSegmentNum = oneRepr.getFirstSegmentNum();
            int lastSegmentNum = oneRepr.getLastSegmentNum(C.UNKNOWN_TIME_US);
            int segmentCount = 0;

            for (int i = firstSegmentNum; i <= lastSegmentNum; ++i) {
                ArrayList<Pair<Format, Uri>> resolutions = new ArrayList<>(3);

                // constructing a list of format-resolutions pairs
                for (Representation repr_ : reprs) {
                    MultiSegmentRepresentation repr = (MultiSegmentRepresentation) repr_;
                    Uri uri = repr.getSegmentUrl(i).getUri();

                    resolutions.add(new Pair<>(repr.format, uri));
                }

                Streamlet streamlet = new Streamlet(this.video,
                        this.storageDir, resolutions);
                streamlet.setStatus(PENDING);

                jobService.submitJob(new SegmentDownloadJob(this, streamlet));
                ++segmentCount;
            }

            Timber.d("Downloaded MPD with %d segments", segmentCount);
        } catch (Exception e) {
            Timber.e(e, "Error processing MPD");
            throw new StreamingException("Error processing MPD", e, getVideo().getVideoId());
        }
    }

    /**
     * To be called by the job queue.
     * <p>
     * Download a segment with an appropriate quality level, recording the download statistics to
     * adaptively choose the quality level for the next download. Will ignore if the segment is
     * no longer needed (esp. in live streaming).
     */
    public void _jobPerformDownloadSegment(Streamlet streamlet) {
        if (!isProgressing()) {
            return;
        }

        // todo select quality based on last speed


        Pair<Format, Uri> resolution = streamlet.getQualities().get(0);
        String path = resolution.second.toString();
        // just take the part of the path after the prefix
        if (path.startsWith(Config.VIDEO_FILES_PREFIX)) {
            path = path.substring(Config.VIDEO_FILES_PREFIX.length() + 1);
        }

        File file = streamlet.getTargetFile();

        try {
            if (!file.exists() || !file.isFile()) {
                file.createNewFile();
            }

            // Do some statistics here
            long startTime = SystemClock.elapsedRealtime();

            Pair<InputStream, Long> downloading = streamingService.getStreamlet(path);

            try (InputStream in = downloading.first;
                 FileOutputStream out = new FileOutputStream(file)) {

                IOUtils.copy(in, out);
            }

            // todo push the streamlet into a queue

            long endTime = SystemClock.elapsedRealtime();
            long duration = endTime - startTime;
            duration = duration == 0 ? 1 : duration;

            long contentLength = downloading.second;

            float speed = (float) contentLength / duration;
            this.lastSpeed = speed;

            streamlet.setStatus(DOWNLOADED);
            Timber.d("Downloaded file: %s, at speed %s bps", path, speed);

        } catch (IOException e) {
            streamlet.setStatus(ERROR);

            Timber.e(e, "Error while streaming streamlet from '%s'", path);
            throw new StreamingException("Error streaming segment from " + path, e,
                    getVideo().getVideoId());
        }
    }

    public interface StateChangeListener {

        void stateChanged(StreamingState newState);
    }


    public static class Streamlet {

        @Getter
        private final Video video;

        @Getter
        private final List<Pair<Format, Uri>> qualities;

        @Getter
        private final File targetFile;

        @Getter
        @Setter
        private Format selectedFormat;

        @Getter
        @Setter
        private Status status;

        public Streamlet(Video video,
                         File videoDir,
                         List<Pair<Format, Uri>> qualities) {

            this.video = video;
            this.qualities = qualities;
            this.status = Status.PENDING;
            this.selectedFormat = null;

            // create a place holder file object (for this stream to be downloaded later)
            // file name matching the last part of the URI
            Uri oneUri = qualities.get(0).second;
            String fileName = oneUri.getLastPathSegment();
            this.targetFile = new File(videoDir, fileName);
        }

        public enum Status {

            PENDING,

            DOWNLOADED,

            ERROR

        }

    }
}

package com.cs5248.android.service;

import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;

import com.cs5248.android.Config;
import com.cs5248.android.model.Video;
import com.cs5248.android.service.job.DownloadJob;
import com.cs5248.android.service.job.FileRemoveJob;
import com.cs5248.android.service.job.MpdDownloadJob;
import com.cs5248.android.service.job.SegmentDownloadJob;
import com.google.android.exoplayer.C;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.dash.mpd.AdaptationSet;
import com.google.android.exoplayer.dash.mpd.Period;
import com.google.android.exoplayer.dash.mpd.Representation;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import timber.log.Timber;

import static com.cs5248.android.service.StreamingService.GetStreamletResult;
import static com.cs5248.android.service.StreamingService.StreamMpdResult;
import static com.cs5248.android.service.StreamingSession.Streamlet.Quality;
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
    private final boolean liveStreaming;

    @Getter
    private volatile StreamingState streamingState;

    /**
     * Use WeakReference to avoid memory leak by the background threads.
     */
    private WeakReference<StreamingListener> streamingListener;

    /**
     * To store the downloaded streamlets
     */
    private final LinkedList<Streamlet> buffer;

    /**
     * Stores the last segment ID returned from the server MPD. So that the next MPD only needs to
     * return the new segments.
     */
    private Long lastSegmentId;

    /**
     * To test whether a new streamlet is a new one, compare its name to the name of the last
     * obtained streamlet.
     */
    private String lastStreamletName;

    /**
     * The quality index used for choosing the quality level for the next segment download.
     */
    private int qualityIndex = 0;


    protected StreamingSession(Context context,
                               StreamingService streamingService,
                               JobService jobService,
                               Video video,
                               File storageDir,
                               boolean liveStreaming) {
        // validate
        Objects.requireNonNull(video);
        Objects.requireNonNull(video.getVideoId());
        Objects.requireNonNull(storageDir);

        this.streamingService = streamingService;
        this.jobService = jobService;
        this.video = video;
        this.storageDir = storageDir;
        this.liveStreaming = liveStreaming;
        this.buffer = new LinkedList<>();

        this.streamingState = NOT_STARTED;
    }

    public final void startStreaming() {
        if (streamingState != NOT_STARTED) {
            Timber.w("Cannot start streaming when state=%s", streamingState);
            return;
        }

        streamingState = PROGRESSING;

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
        streamingState = ENDED;
    }

    /**
     * Cleans up the storage directory after playback.
     * And remove all pending jobs for streaming.
     */
    private void cleanUp() {
        jobService.removeJobByTag(DownloadJob.STREAMING_JOB_TAG);
        jobService.submitJob(new FileRemoveJob(storageDir, true));
    }

    public boolean isProgressing() {
        return getStreamingState() == PROGRESSING;
    }

    private void streamletDownloaded(Streamlet streamlet) {
        synchronized (buffer) {
            // since all download jobs are executed in sequence, we can be
            // sure that the streamlet are downloaded in order.
            buffer.push(streamlet);
        }

        StreamingListener streamingListener = getStreamingListener();
        if (streamingListener != null) {
            streamingListener.streamletDownloaded(streamlet);
        }
    }

    /**
     * Mark the end of the streaming session because the stream has ended.
     */
    private void noMoreStreamlet() {
        StreamingListener streamingListener = getStreamingListener();
        if (streamingListener != null) {
            streamingListener.noMoreStreamlet();
        }
    }

    /**
     * Called by the client once it's done with the streamlet so we can delete
     * the streamlet's file.
     */
    public void clearStreamlet(Streamlet streamlet) {
        jobService.submitJob(new FileRemoveJob(streamlet.getTargetFile(), false));
    }

    /**
     * Called by the client to poll for the next available streamlet. This method will return
     * right away. Returns null if there is no available streamlet.
     */
    public Streamlet getNextStreamlet() {
        return buffer.poll();
    }

    public void setStreamingListener(StreamingListener listener) {
        this.streamingListener = new WeakReference<>(listener);
    }

    public StreamingListener getStreamingListener() {
        WeakReference<StreamingListener> ref = this.streamingListener;
        if (ref == null) {
            return null;
        }

        return ref.get();
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

        StreamMpdResult mpdResult = streamingService.getMpd(getVideo(), lastSegmentId);

        if (mpdResult == null || mpdResult.mpd == null) {
            throw new StreamingException("Unable to download MPD for video", getVideo().getVideoId());
        }

        if (mpdResult.lastSegmentId != null) {
            this.lastSegmentId = mpdResult.lastSegmentId;
        }

        boolean streamEnded = Boolean.TRUE.equals(mpdResult.isFinalSet);

        try {
            Period period = mpdResult.mpd.getPeriod(0);
            AdaptationSet adaptationSet = period.adaptationSets.get(0);
            ArrayList<Representation> reprs = new ArrayList<>(adaptationSet.representations);

            // ensures we have all three representations
            if (reprs.size() < 3) {
                throw new IllegalStateException("Expected 3 representations in the MPD");
            }

            // sort the reprs by descending bitrates;
            Collections.sort(reprs,
                    (repr1, repr2) -> repr2.format.bitrate - repr1.format.bitrate);


            // get the segments
            MultiSegmentRepresentation oneRepr = (MultiSegmentRepresentation) reprs.get(0);
            int firstSegmentNum = oneRepr.getFirstSegmentNum();
            int lastSegmentNum = oneRepr.getLastSegmentNum(C.UNKNOWN_TIME_US);
            int segmentCount = 0;

            SegmentLoop:
            for (int i = firstSegmentNum; i <= lastSegmentNum; ++i) {
                ArrayList<Quality> qualities = new ArrayList<>(3);

                // constructing a list of format-uri pairs
                for (Representation repr_ : reprs) {
                    MultiSegmentRepresentation repr = (MultiSegmentRepresentation) repr_;
                    Uri uri = repr.getSegmentUrl(i).getUri();
                    qualities.add(new Quality(repr.format, uri));
                }

                // get the media name (last part of URI) and use that to
                // know if this streamlet has been downloaded before
                String streamletName = qualities.get(0).uri.getLastPathSegment();
                if (lastStreamletName != null && streamletName.compareTo(lastStreamletName) <= 0) {
                    // no longer need this segment
                    continue SegmentLoop;
                }

                lastStreamletName = streamletName;

                boolean isLastStreamlet = false;
                if (streamEnded && i == lastSegmentNum) {
                    isLastStreamlet = true;
                }

                Streamlet streamlet = new Streamlet(this.video,
                        this.storageDir,
                        qualities,
                        streamletName,
                        isLastStreamlet);

                streamlet.setStatus(PENDING);

                jobService.submitJob(new SegmentDownloadJob(this, streamlet));
                ++segmentCount;
            }

            Timber.d("Downloaded MPD with %d segments", segmentCount);
        } catch (Exception e) {
            Timber.e(e, "Error processing MPD");
            throw new StreamingException("Error processing MPD", e, getVideo().getVideoId());
        }

        // if this is live streaming, schedule the next MPD update
        if (liveStreaming) {
            jobService.submitJob(new MpdDownloadJob(this,
                    // delay for half the duration of the segment
                    (int) (getVideo().getSegmentDuration() * .5f)));
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

        // choose a quality level based on the quality index adjusted from the last download
        streamlet.setSelectedQuality(streamlet.getQualities().get(this.qualityIndex));

        Quality quality = streamlet.getSelectedQuality();
        String path = quality.uri.toString();
        // just take the part of the path after the prefix
        if (path.startsWith(Config.VIDEO_FILES_PREFIX)) {
            path = path.substring(Config.VIDEO_FILES_PREFIX.length());
        }

        File file = streamlet.getTargetFile();

        try {
            if (!file.exists() || !file.isFile()) {
                file.createNewFile();
            }

            // Do some statistics here
            long startTime = SystemClock.elapsedRealtime();

            GetStreamletResult getStreamletResult = streamingService.getStreamlet(path);

            try (InputStream in = getStreamletResult.stream;
                 FileOutputStream out = new FileOutputStream(file)) {

                IOUtils.copy(in, out);
            }

            // calculate the downloading duration
            long endTime = SystemClock.elapsedRealtime();
            long duration = endTime - startTime;

            // check if we need to increase / decrease the quality index
            long allowedDuration = getVideo().getSegmentDuration();
            if (duration >= allowedDuration * .8f) {
                // if the streamlet took longer time to download, decrease the quality level for the
                // next streamlet
                ++this.qualityIndex;
                int maxIndex = streamlet.getQualities().size() - 1;
                if (this.qualityIndex > maxIndex) {
                    this.qualityIndex = maxIndex;
                }
            } else if (duration < allowedDuration / 2) {
                // the network seems to be fast, increase quality level
                --this.qualityIndex;
                if (this.qualityIndex < 0) {
                    this.qualityIndex = 0;
                }
            }

            streamlet.setStatus(DOWNLOADED);
            Timber.d("Downloaded file: %s. Duration: %dms.Next quality index: %d",
                    path, duration, this.qualityIndex);

            // update the client
            streamletDownloaded(streamlet);

        } catch (IOException e) {
            streamlet.setStatus(ERROR);

            Timber.e(e, "Error while streaming streamlet from '%s'", path);
            throw new StreamingException("Error streaming segment from " + path, e,
                    getVideo().getVideoId());

        } finally {
            // signal the end of stream
            if (streamlet.isLast()) {
                noMoreStreamlet();
            }
        }
    }

    public interface StreamingListener {

        void streamletDownloaded(Streamlet streamlet);

        void noMoreStreamlet();

    }


    public static class Streamlet implements Serializable {

        @Getter
        private final Video video;

        @Getter
        private final List<Quality> qualities;

        @Getter
        private final File targetFile;

        @Getter
        private final String mediaName;

        @Getter
        private final boolean last;

        @Getter
        @Setter
        private Quality selectedQuality;

        @Getter
        @Setter
        private Status status;

        public Streamlet(Video video,
                         File videoDir,
                         List<Quality> qualities,
                         String mediaName,
                         boolean isLast) {

            this.video = video;
            this.qualities = qualities;
            this.status = Status.PENDING;
            this.selectedQuality = null;
            this.mediaName = mediaName;
            this.last = isLast;

            // create a place holder file object (for this stream to be downloaded later)
            // file name matching the last part of the URI
            this.targetFile = new File(videoDir, mediaName);
        }

        public enum Status {

            PENDING,

            DOWNLOADED,

            ERROR

        }

        public static class Quality {

            public final Format format;

            public final Uri uri;

            public Quality(Format format, Uri uri) {
                this.format = format;
                this.uri = uri;
            }

        }

    }
}

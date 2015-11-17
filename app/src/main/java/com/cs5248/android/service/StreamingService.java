package com.cs5248.android.service;

import android.content.Context;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Pair;

import com.cs5248.android.model.Video;
import com.cs5248.android.model.VideoType;
import com.cs5248.android.service.job.StoragePrepareJob;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescription;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescriptionParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import timber.log.Timber;

/**
 * The service used for initiating a streaming session.
 *
 * @author lpthanh
 */
public class StreamingService {

    private final Context context;

    private final ApiService apiService;

    private final JobService jobService;

    private final MediaPresentationDescriptionParser mpdParser;

    private File downloadDir;


    public StreamingService(Context context,
                            ApiService apiService,
                            JobService jobService) {

        this.context = context;
        this.apiService = apiService;
        this.jobService = jobService;
        this.mpdParser = new MediaPresentationDescriptionParser();

        this.downloadDir = prepareDownloadDir();
    }

    private File prepareDownloadDir() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return null;
        }

        File extDir = new File(Environment.getExternalStorageDirectory(), "cs5248-android");
        File downloadDir = new File(extDir, "downloaded");
        if (downloadDir.exists()) {
            // clean up
            this.cleanUpLeftOverAsync(downloadDir);
        } else {
            if (!downloadDir.mkdirs() || !downloadDir.isDirectory()) {
                return null;
            }
        }

        return downloadDir;
    }

    /**
     * Obtain an empty directory for storing temporary files used for streaming the video.
     */
    private File getDownloadDirForVideo(Video video) {
        if (downloadDir == null) {
            // try again
            downloadDir = prepareDownloadDir();

            // the dir is really just not available
            if (downloadDir == null) {
                throw new StreamingException("The download directory '" +
                        downloadDir.getAbsolutePath() + "' is not available",
                        video.getVideoId());
            }
        }

        File dirForVideo;
        do {
            // find one directory that is not yet existing
            dirForVideo = new File(downloadDir,
                    "video-" + SystemClock.elapsedRealtime() + "-" + video.getVideoId());
        } while (dirForVideo.exists() && dirForVideo.isDirectory());

        if (!dirForVideo.mkdirs() || !dirForVideo.isDirectory()) {
            throw new RuntimeException("Could not create directory: " + dirForVideo);
        }

        return dirForVideo;
    }


    /**
     * Delete the files left behind by the previous streaming session, asynchronously
     */
    private void cleanUpLeftOverAsync(File storageDir) {
        jobService.submitJob(new StoragePrepareJob(storageDir));
    }

    /**
     * @return the tuple containing the parsed MPD and the lastSegmentId returned by server.
     */
    public StreamMpdResult getMpd(Video video, Long lastSegmentId) {

        Pair<InputStream, Pair<Long, Boolean>> response = apiService.streamMpd(video.getVideoId(), lastSegmentId);
        if (response == null) {
            return null;
        }

        try (InputStream mpdStream = response.first) {
            if (mpdStream == null) {
                return null;
            }

            MediaPresentationDescription mpd = mpdParser.parse(video.getBaseUrl(), mpdStream);
            return new StreamMpdResult(mpd, response.second.first, response.second.second);

        } catch (Exception e) {
            Timber.e(e, "Error reading MPD stream");
            return null;
        }
    }

    /**
     * @return a tuple of InputStream and the content length
     */
    public GetStreamletResult getStreamlet(String path) throws IOException {
        Pair<InputStream, Long> response =  apiService.streamVideoFile(path);
        return new GetStreamletResult(response.first, response.second);
    }

    public StreamingSession openSession(Video video, boolean live) {
        if (live ^ video.getType() == VideoType.LIVE) {
            throw new StreamingException("Video type and streaming type do not match. Expecting live=" + live,
                    video.getVideoId());
        }

        return new StreamingSessionImpl(video, live);
    }

    private class StreamingSessionImpl extends StreamingSession {

        public StreamingSessionImpl(Video video, boolean live) {
            super(context,
                    StreamingService.this,
                    StreamingService.this.jobService,
                    video,
                    getDownloadDirForVideo(video),
                    live);
        }
    }

    /**
     * Temporary container for streaming an MPD.
     */
    public static class StreamMpdResult {

        public final MediaPresentationDescription mpd;

        public final Long lastSegmentId;

        public final Boolean isFinalSet;

        public StreamMpdResult(MediaPresentationDescription mpd,
                               Long lastSegmentId,
                               Boolean isFinalSet) {

            this.mpd = mpd;
            this.lastSegmentId = lastSegmentId;
            this.isFinalSet = isFinalSet;
        }

    }

    /**
     * A temporary container for the results of streaming a streamlet video file.
     */
    public static class GetStreamletResult {

        public final InputStream stream;

        public final long contentLength;

        public GetStreamletResult(InputStream stream, long contentLength) {
            this.stream = stream;
            this.contentLength = contentLength;
        }

    }

}

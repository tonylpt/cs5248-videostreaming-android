package com.cs5248.android.service;

import android.content.Context;
import android.os.Environment;
import android.os.SystemClock;

import com.cs5248.android.model.Video;
import com.cs5248.android.model.VideoSegment;

import java.io.File;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import timber.log.Timber;

import static com.cs5248.android.service.StreamingState.ENDED;
import static com.cs5248.android.service.StreamingState.NOT_STARTED;
import static com.cs5248.android.service.StreamingState.PROGRESSING;

/**
 * @author lpthanh
 */
public abstract class StreamingSession {

    private final StreamingService service;

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

    protected StreamingSession(Context context, StreamingService service, Video video) {
        // validate
        Objects.requireNonNull(video);
        Objects.requireNonNull(video.getVideoId());

        this.service = service;
        this.video = video;
        this.streamingState = NOT_STARTED;

        this.storageDir = prepareTempDirForVideo(video);
    }

    protected abstract void onStreamingStarted(Video video);

    protected abstract void onStreamingEnded(Video video);

    protected abstract void onSegmentDownloaded(Video video, VideoSegment segment);

    protected abstract void onSegmentPlayed(Video video, VideoSegment segment);


    public final void startStreaming() {
        if (streamingState != NOT_STARTED) {
            Timber.w("Cannot start streaming when state=%s", streamingState);
            return;
        }

        setStreamingState(PROGRESSING);
        onStreamingStarted(video);
    }

    public final void endStreaming() {
        if (streamingState != PROGRESSING) {
            Timber.w("Cannot end streaming when state=%s", streamingState);
            return;
        }

        streamingEnded();
    }

    private void segmentDownloaded(VideoSegment segment) {
        currentSegment = segment;
        onSegmentDownloaded(video, segment);
    }

    private void streamingEnded() {
        setStreamingState(ENDED);
        onStreamingEnded(video);
        currentSegment = null;
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

    private static File prepareTempDirForVideo(Video video) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            throw new RuntimeException("No external storage is available.");
        }

        File extDir = new File(Environment.getExternalStorageDirectory(), "cs5248-android");
        File videoDir = new File(extDir, "downloaded/video-" + SystemClock.elapsedRealtime() + "-" + video.getVideoId());
        if (!videoDir.exists()) {
            if (!videoDir.mkdirs() || !videoDir.isDirectory()) {
                throw new RuntimeException("Could not create directory: " + videoDir);
            }
        }

        return videoDir;
    }

    public boolean isProgressing() {
        return getStreamingState() == PROGRESSING;
    }

    public interface StateChangeListener {

        void stateChanged(StreamingState newState);
    }

}

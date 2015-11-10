package com.cs5248.android.service;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Environment;
import android.os.SystemClock;

import com.cs5248.android.model.Video;
import com.cs5248.android.model.VideoSegment;
import com.cs5248.android.ui.CameraPreviewer;
import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import lombok.Getter;
import lombok.Setter;
import timber.log.Timber;

import static com.cs5248.android.service.RecordingState.ENDED;
import static com.cs5248.android.service.RecordingState.NOT_STARTED;
import static com.cs5248.android.service.RecordingState.PROGRESSING;

/**
 * @author lpthanh
 */
public abstract class Recording {

    private final AtomicLong nextSegmentId = new AtomicLong();

    private final RecordingService service;

    private final SimulationThread simulationThread;

    @Getter
    private final Video video;

    @Getter
    private final File recordDir;

    @Getter
    private VideoSegment currentSegment;

    @Getter
    private volatile RecordingState recordingState;

    @Setter
    private StateChangeListener stateChangeListener;

    protected Recording(Context context, RecordingService service, Video video) {
        // validate
        Objects.requireNonNull(video);
        Objects.requireNonNull(video.getVideoId());

        this.service = service;
        this.simulationThread = new SimulationThread(context, this, 10, 3000);
        this.video = video;
        this.recordingState = NOT_STARTED;

        this.recordDir = prepareTempDirForVideo(video);
    }

    protected abstract void onRecordingStarted(Video video);

    protected abstract void onRecordingEnded(Video video);

    protected abstract void onSegmentRecorded(Video video, VideoSegment segment);


    public final void startRecording() {
        if (recordingState != NOT_STARTED) {
            Timber.w("Cannot start recording when state=%s", recordingState);
            return;
        }

        setRecordingState(PROGRESSING);
        simulationThread.start();
        onRecordingStarted(video);
    }

    public final void endRecording() {
        if (recordingState != PROGRESSING) {
            Timber.w("Cannot end recording when state=%s", recordingState);
            return;
        }

        setRecordingState(ENDED);
        simulationThread.stopSimulating();
        onRecordingEnded(video);
    }

    private void segmentRecorded(VideoSegment segment) {
        currentSegment = segment;
        onSegmentRecorded(video, segment);
    }

    private void recordingEnded() {
        setRecordingState(ENDED);
        onRecordingEnded(video);
    }

    private void recordingEndedWithError(Throwable error) {
        Timber.e(error, "An error occurred during recording.");
        recordingEnded();

        // todo handle error
    }

    private VideoSegment createNextSegment() {
        VideoSegment segment = new VideoSegment();
        segment.setVideoId(video.getVideoId());
        segment.setSegmentId(nextSegmentId.getAndAdd(1));
        segment.setOriginalExtension("mp4");
        return segment;
    }

    public void setPreviewer(CameraPreviewer previewer) {

    }

    public Camera initCamera(int frameWidth, int frameHeight, int fps) {
        Camera camera = null;
        Camera.CameraInfo info = new Camera.CameraInfo();
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i <
                numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                camera = Camera.open(i);
                break;
            }
        }
        if (camera == null) {
            camera = Camera.open();
        }
        if (camera == null) {
            throw new RuntimeException("Unable to open camera");
        }

        Camera.Parameters parms = camera.getParameters();
        parms.setPreviewFormat(ImageFormat.YV12);
        parms.setPreviewSize(frameWidth, frameHeight);
        parms.setPreviewFrameRate(fps);
        camera.setParameters(parms);

        return camera;
    }

    private void setRecordingState(RecordingState newState) {
        RecordingState lastState = this.recordingState;
        this.recordingState = newState;
        if (lastState != newState) {
            if (stateChangeListener != null) {
                try {
                    stateChangeListener.stateChanged(recordingState);
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
        File videoDir = new File(extDir, "recorded/video-" + SystemClock.elapsedRealtime() + "-" + video.getVideoId());
        if (!videoDir.exists()) {
            if (!videoDir.mkdirs() || !videoDir.isDirectory()) {
                throw new RuntimeException("Could not create directory: " + videoDir);
            }
        }

        return videoDir;
    }

    public interface StateChangeListener {

        void stateChanged(RecordingState newState);
    }

    /**
     * Fake produce video files for uploading
     */
    private static class SimulationThread extends Thread {

        private final Recording recording;

        private final AssetManager assets;

        private final int totalSegment;

        private final int producedSegment;

        private final long interval;

        public SimulationThread(Context context, Recording recording, int totalSegment, long interval) {
            if (totalSegment <= 0) {
                throw new IllegalArgumentException();
            }

            this.recording = recording;
            this.assets = context.getAssets();
            this.totalSegment = totalSegment;
            this.producedSegment = 0;
            this.interval = interval;
        }

        @Override
        public void run() {
            while (!interrupted() || producedSegment <= totalSegment) {
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    break;
                }

                // produce the video file
                VideoSegment segment = recording.createNextSegment();
                String fileName = segment.getSegmentId() + "." + segment.getOriginalExtension();
                File outputFile = new File(recording.recordDir, fileName);
                segment.setOriginalPath(outputFile.getAbsolutePath());

                try {
                    if (!outputFile.createNewFile()) {
                        throw new IOException("Unable to create a new file: " + outputFile);
                    }

                    try (InputStream in = assets.open("test_video/test_video.mp4");
                         FileOutputStream out = new FileOutputStream(outputFile)) {

                        // copy the file from assets directory to a new file on external storage
                        ByteStreams.copy(in, out);
                    }

                } catch (Exception e) {
                    recording.recordingEndedWithError(e);
                    return;
                }

                recording.segmentRecorded(segment);
            }

            recording.recordingEnded();
        }

        public void stopSimulating() {
            interrupt();
        }
    }
}

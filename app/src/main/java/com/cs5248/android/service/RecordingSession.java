package com.cs5248.android.service;

import android.content.Context;
import android.content.res.AssetManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.FileObserver;
import android.os.SystemClock;

import com.cs5248.android.model.Video;
import com.cs5248.android.model.VideoSegment;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
public abstract class RecordingSession {

    private final AtomicLong nextSegmentId = new AtomicLong();

    private final ExecutorService cameraExec =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "camera-thread"));

    private final RecordingService service;

//    private final SimulationThread simulationThread;

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

    private ChunkedRecorderWrapper recorderWrapper;


    protected RecordingSession(Context context, RecordingService service, Video video) {
        // validate
        Objects.requireNonNull(video);
        Objects.requireNonNull(video.getVideoId());

        this.service = service;
//        this.simulationThread = new SimulationThread(context, this, 4, 3000);
        this.video = video;
        this.recordingState = NOT_STARTED;

        this.recordDir = prepareTempDirForVideo(video);
    }

    protected abstract void onRecordingStarted(Video video);

    protected abstract void onRecordingEnded(Video video, VideoSegment lastSegment);

    protected abstract void onSegmentRecorded(Video video, VideoSegment segment);


    public final void startRecording(Camera camera,
                                     CamcorderProfile profile,
                                     MediaRecorder recorder,
                                     int segmentDuration,
                                     int segmentsPerStreak) {

        if (recordingState != NOT_STARTED) {
            Timber.w("Cannot start recording when state=%s", recordingState);
            return;
        }

        setRecordingState(PROGRESSING);
//        simulationThread.start();
        onRecordingStarted(video);
        recorderWrapper = new ChunkedRecorderWrapper(camera,
                profile,
                recorder,
                segmentDuration,
                segmentsPerStreak);
    }

    public final void endRecording() {
        if (recordingState != PROGRESSING) {
            Timber.w("Cannot end recording when state=%s", recordingState);
            return;
        }

//        simulationThread.stopSimulating();
        recorderWrapper.stop();
    }

    private void segmentRecorded(VideoSegment segment) {
        currentSegment = segment;
        onSegmentRecorded(video, segment);
    }

    private void recordingEnded() {
        onRecordingEnded(video, currentSegment);
        currentSegment = null;
        setRecordingState(ENDED);
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

    public boolean isProgressing() {
        return getRecordingState() == PROGRESSING;
    }

    public void runOnCameraThread(Runnable runnable, boolean wait) {
        if (wait) {
            Future f = cameraExec.submit(runnable);
            try {
                f.get();
            } catch (ExecutionException e) {
                Timber.w(e, "Error executing camera task");
            } catch (InterruptedException e) {
                Timber.w(e, "Interrupted!");
            }
        } else {
            cameraExec.execute(runnable);
        }
    }

    public void runOnCameraThread(Runnable runnable) {
        this.runOnCameraThread(runnable, false);
    }

    public void dispose() {
        cameraExec.shutdown();
    }

    public interface StateChangeListener {

        void stateChanged(RecordingState newState);
    }

    private class ChunkedRecorderWrapper implements MediaRecorder.OnInfoListener {

        private final Camera camera;

        private final CamcorderProfile profile;

        private final MediaRecorder mediaRecorder;

        private final int segmentDuration;

        private final int streakDuration;

        private FileObserver currentFileObserver;

        private VideoSegment currentSegment;

        private volatile String lastSegmentPath;

        private volatile long lastStartMillis;

        private volatile boolean recording;

        private volatile boolean stopped;

        private final Object recorderLock = new Object();

        public ChunkedRecorderWrapper(Camera camera,
                                      CamcorderProfile profile,
                                      MediaRecorder mediaRecorder,
                                      int segmentDuration,
                                      int segmentsPerStreak) {

            this.camera = camera;
            this.profile = profile;
            this.mediaRecorder = mediaRecorder;
            this.segmentDuration = segmentDuration;
            this.streakDuration = segmentDuration * segmentsPerStreak;
//            this.camera.release();
            reinitRecorder();
        }

        private void reinitRecorder() {
            synchronized (recorderLock) {
                camera.unlock();
                mediaRecorder.setCamera(camera);
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
                mediaRecorder.setProfile(profile);
                mediaRecorder.setMaxDuration(streakDuration);
                mediaRecorder.setOnInfoListener(this);

                VideoSegment segment = this.currentSegment = createNextSegment();
                String fileName = "video-" + segment.getSegmentId() + "." + segment.getOriginalExtension();

                File outputFile = new File(recordDir, fileName);
                segment.setOriginalPath(outputFile.getAbsolutePath());

                try {
                    if (!outputFile.createNewFile()) {
                        throw new IOException("Unable to create a new file: " + outputFile);
                    }

                    String outputPath = outputFile.getAbsolutePath();
                    mediaRecorder.setOutputFile(outputPath);

                    observeWriteEnd(outputPath);

                    mediaRecorder.prepare();
                    mediaRecorder.start();

                    recording = true;
                    lastStartMillis = SystemClock.elapsedRealtime();

                    Timber.d("Recording into file %s", outputFile.getAbsolutePath());
                } catch (Exception e) {
                    Timber.e(e, "Error initializing MediaRecorder.");
                    recordingEndedWithError(e);
                }
            }
        }

        private void stop() {
            lastSegmentPath = currentSegment.getOriginalPath();
            runOnCameraThread(() -> {
                try {
                    synchronized (recorderLock) {
                        stopped = true;
                        if (recording) {

                            // stop() cannot be too soon after start()
                            long durationFromStart = SystemClock.elapsedRealtime() - lastStartMillis;
                            if (durationFromStart < 1000) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    Timber.e(e, "Interrupted!");
                                }
                            }

                            mediaRecorder.stop();
                            mediaRecorder.reset();
                        }

                        Timber.d("Releasing media recorder");
                        mediaRecorder.release();
                    }

                } catch (Throwable e) {
                    Timber.e(e, "Error stopping media recorder");
                }
            });
        }

        @Override
        public void onInfo(MediaRecorder mr, int what, int extra) {
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                // this handler is already on camera thread
                try {
                    synchronized (recorderLock) {
                        recording = false;
                        mediaRecorder.stop();
                        mediaRecorder.reset();
                    }
                } catch (Throwable e) {
                    Timber.e(e, "Error stopping media recorder");
                }
            }
        }

        /**
         * Since MediaRecorder.stop() is async, this method is needed to make sure the file
         * has been properly finalized.
         */
        private void observeWriteEnd(final String path) {
            this.currentFileObserver = new FileObserver(path) {
                @Override
                public void onEvent(int event, String ignored) {
                    if (event == CLOSE_WRITE) {
                        try {
//                            if (lastSegmentPath != null && path.equals(lastSegmentPath)) {
//                                // end the whole recording process
//                                runOnCameraThread(() -> {
//                                    try {
//                                        // give itc some time to finalize things
//                                        Thread.sleep(500);
//                                    } catch (InterruptedException e) {
//                                        Timber.w(e, "Interrupted!");
//                                    }
//
//                                    try {
//
//                                    } catch (Throwable e) {
//                                        Timber.e(e, "Error releasing recorder");
//                                    }
//                                }, true);
                            if (stopped) {
                                segmentRecorded(currentSegment);
                                recordingEnded();

                            } else {
                                // end this segment and start a new one
                                segmentRecorded(currentSegment);
                                reinitRecorder();
                            }
                            stopWatching();
                        } catch (Throwable e) {
                            Timber.e(e, "Error closing the segment file");
                        }
                    }
                }
            };

            this.currentFileObserver.startWatching();
        }
    }

    /**
     * Fake produce video files for uploading
     */
    private static class SimulationThread extends Thread {

        private final RecordingSession recordingSession;

        private final AssetManager assets;

        private final long interval;

        private final int totalSegment;

        private int producedSegment;

        public SimulationThread(Context context, RecordingSession recordingSession, int totalSegment, long interval) {
            if (totalSegment <= 0) {
                throw new IllegalArgumentException();
            }

            this.recordingSession = recordingSession;
            this.assets = context.getAssets();
            this.totalSegment = totalSegment;
            this.producedSegment = 0;
            this.interval = interval;
        }

        private static String randomFile() {
            if (Math.random() > .5) {
                return "test_video/test_video.mp4";
            } else {
                return "test_video/test_video_2.mp4";
            }
        }

        @Override
        public void run() {
            String filePath = randomFile();

            while (!interrupted() && producedSegment <= totalSegment) {
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    break;
                }

                // produce the video file
                VideoSegment segment = recordingSession.createNextSegment();

                ++producedSegment;

                String fileName = segment.getSegmentId() + "." + segment.getOriginalExtension();
                File outputFile = new File(recordingSession.recordDir, fileName);
                segment.setOriginalPath(outputFile.getAbsolutePath());

                try {
                    if (!outputFile.createNewFile()) {
                        throw new IOException("Unable to create a new file: " + outputFile);
                    }

                    try (InputStream in = assets.open(filePath);
                         FileOutputStream out = new FileOutputStream(outputFile)) {

                        // copy the file from assets directory to a new file on external storage
                        IOUtils.copy(in, out);
                    }

                } catch (Exception e) {
                    recordingSession.recordingEndedWithError(e);
                    return;
                }

                recordingSession.segmentRecorded(segment);
            }

            recordingSession.recordingEnded();
        }

        public void stopSimulating() {
            interrupt();
        }
    }
}

package com.cs5248.android.service;

import android.content.Context;
import android.content.res.AssetManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.FileObserver;
import android.os.SystemClock;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.cs5248.android.model.Video;
import com.cs5248.android.model.VideoSegment;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
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
 * This class handles all the recording logic for one video.
 *
 * @author lpthanh
 */
public abstract class RecordingSession {

    private final AtomicLong nextSegmentId = new AtomicLong();

    private final AtomicLong nextStreakId = new AtomicLong();

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

    protected abstract void onStreakRecorded(RecordingStreak streak);


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

    private RecordingStreak createNextStreak(int segmentDuration) {

        return new RecordingStreak(video.getVideoId(),
                nextStreakId.getAndAdd(1),
                segmentDuration,
                recordDir,
                "mp4");
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

    /**
     * A streak consists of a group of consecutive segments. We record in streaks and then divide
     * that into segments so that we can minimize the number of gaps between the segments. Segments
     * within a streak ideally do not have gaps between them. There is always a gap between streaks
     * (and hence the segment at the end of one streak and the segment at the beginning of the next
     * streak), because of the way MediaRecorder is stopped and restarted for each streak.
     * <p>
     * This was an improvement after the project has been submitted, just so I can test the continuity
     * of the player's MediaPlayer switching.
     * <p>
     * Much of the segmentation logic was adapted from edwinbs's code.
     */
    public class RecordingStreak {

        @Getter
        private final long videoId;

        @Getter
        private final long streakId;

        @Getter
        private final float segmentDuration;

        @Getter
        private final String extension;

        @Getter
        private final File storageDir;

        @Getter
        private final File recordFile;

        @Getter
        @Setter
        private boolean lastStreak;

        public RecordingStreak(long videoId,
                               long streakId,
                               int segmentDurationMillis,
                               File storageDir,
                               String extension) {

            this.videoId = videoId;
            this.streakId = streakId;
            this.segmentDuration = segmentDurationMillis / 1000.0f;
            this.storageDir = storageDir;
            this.extension = extension;

            String fileName = "streak-" + streakId + "." + extension;
            this.recordFile = new File(storageDir, fileName);
        }

        public void _jobSegmentStreak() {
            if (recordFile == null || !recordFile.isFile()) {
                throw new SegmentationException("File does not exist: " + recordFile.getAbsolutePath(), videoId);
            }

            long timerStart = System.currentTimeMillis();

            try (FileInputStream movieStream = new FileInputStream(recordFile)) {
                Movie movie = MovieCreator.build(movieStream.getChannel());
                Track videoTrack = findVideoTrack(movie.getTracks());
                if (videoTrack == null) {
                    throw new SegmentationException("Cannot find any video track: " +
                            recordFile.getAbsolutePath(), videoId);
                }

                double startTime = correctTimeToSyncSample(videoTrack, 0.0, false);
                double endTime = correctTimeToSyncSample(videoTrack, startTime + this.segmentDuration, true);

                List<Track> originalTracks = movie.getTracks();

                while (startTime < endTime) {
                    movie.setTracks(new LinkedList<>());
                    addCroppedTracks(movie, originalTracks, startTime, endTime);

                    VideoSegment segment = createNextSegment();
                    String fileName = "video-" + segment.getSegmentId() + "." + segment.getOriginalExtension();

                    File outputFile = new File(storageDir, fileName);
                    segment.setOriginalPath(outputFile.getAbsolutePath());

                    writeMovieFile(movie, outputFile);

                    segmentRecorded(segment);

                    //Find next segment's start time and end time
                    //If next segment's start is equal to its end (duration=0), then this is the final segment
                    startTime = endTime;
                    endTime = correctTimeToSyncSample(videoTrack, startTime + this.segmentDuration, true);
                }

            } catch (IOException e) {
                throw new SegmentationException("Error cropping movie", e, videoId);
            } finally {
                if (lastStreak) {
                    recordingEnded();
                }
            }

            long timerEnd = System.currentTimeMillis();
            Timber.d("Segmentation finished in %d ms", (timerEnd - timerStart));
        }

        public void _jobCleanupStreakFile() {
            // delete the local file
            File file = getRecordFile();
            if (!file.exists() || !file.isFile()) {
                return;
            }

            if (!file.delete()) {
                Timber.e("Failed to delete file: %s", file.getAbsolutePath());
            }
        }

        private void addCroppedTracks(Movie movie,
                                      List<Track> originalTracks,
                                      double adjustedStartTime,
                                      double adjustedEndTime) {

            movie.setTracks(new LinkedList<>());
            for (Track track : originalTracks) {
                long currentSample = 0;
                double currentTime = 0;
                long startSample = -1;
                long endSample = -1;

                for (TimeToSampleBox.Entry entry : track.getDecodingTimeEntries()) {
                    for (int i = 0; i < entry.getCount(); i++) {
                        if (currentTime <= adjustedStartTime) {
                            // current sample is still before the new start time
                            startSample = currentSample;
                        }

                        if (currentTime <= adjustedEndTime) {
                            // current sample is after the new start time and still before the new end time
                            endSample = currentSample;
                        } else {
                            // current sample is after the end of the cropped video
                            break;
                        }
                        currentTime += (double) entry.getDelta() / (double) track.getTrackMetaData().getTimescale();
                        currentSample++;
                    }
                }
                movie.addTrack(new CroppedTrack(track, startSample, endSample));
            }
        }

        protected void writeMovieFile(Movie movie, File file) throws IOException {
            try {
                if (!file.exists()) {
                    file.createNewFile();
                }

                IsoFile out = new DefaultMp4Builder().build(movie);
                FileOutputStream fos = new FileOutputStream(file.getAbsolutePath());
                FileChannel fc = fos.getChannel();
                out.getBox(fc);
                fc.close();
                fos.close();
            } catch (IOException e) {
                Timber.e(e, "Error writing movie to file %s", file.getAbsolutePath());
            }
        }

        protected Track findVideoTrack(List<Track> tracks) {
            for (Track track : tracks) {
                if (isVideoTrack(track)) {
                    return track;
                }
            }
            return null;
        }

        protected boolean isVideoTrack(Track track) {
            return (track.getMediaHeaderBox().getType().equals("vmhd"));
        }

        protected long getDuration(Track track) {
            long duration = 0;
            for (TimeToSampleBox.Entry entry : track.getDecodingTimeEntries()) {
                duration += entry.getCount() * entry.getDelta();
            }
            return duration;
        }

        private double correctTimeToSyncSample(Track track, double cutHere, boolean next) {
            double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
            long currentSample = 0;
            double currentTime = 0;
            for (int i = 0; i < track.getDecodingTimeEntries().size(); i++) {
                TimeToSampleBox.Entry entry = track.getDecodingTimeEntries().get(i);
                for (int j = 0; j < entry.getCount(); j++) {
                    if (Arrays.binarySearch(track.getSyncSamples(), currentSample + 1) >= 0) {
                        // samples always start with 1 but we start with zero therefore +1
                        timeOfSyncSamples[Arrays.binarySearch(track.getSyncSamples(), currentSample + 1)] = currentTime;
                    }
                    currentTime += (double) entry.getDelta() / (double) track.getTrackMetaData().getTimescale();
                    currentSample++;
                }
            }
            double previous = 0;
            for (double timeOfSyncSample : timeOfSyncSamples) {
                if (timeOfSyncSample > cutHere) {
                    if (next) {
                        return timeOfSyncSample;
                    } else {
                        return previous;
                    }
                }
                previous = timeOfSyncSample;
            }
            return timeOfSyncSamples[timeOfSyncSamples.length - 1];
        }
    }

    private class ChunkedRecorderWrapper implements MediaRecorder.OnInfoListener {

        private final Camera camera;

        private final CamcorderProfile profile;

        private final MediaRecorder mediaRecorder;

        private final int segmentDuration;

        private final int streakDuration;

        private FileObserver currentFileObserver;

        private RecordingStreak currentStreak;

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

                RecordingStreak streak = this.currentStreak = createNextStreak(segmentDuration);
                File outputFile = streak.getRecordFile();

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
                                currentStreak.setLastStreak(true);
                                onStreakRecorded(currentStreak);

                            } else {
                                // end this segment and start a new one
                                currentStreak.setLastStreak(false);
                                onStreakRecorded(currentStreak);
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

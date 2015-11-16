package com.cs5248.android.service;

import android.content.Context;

import com.cs5248.android.model.Video;
import com.cs5248.android.model.VideoSegment;
import com.cs5248.android.model.VideoStatus;
import com.cs5248.android.model.VideoType;
import com.cs5248.android.service.job.SegmentLiveUploadJob;
import com.cs5248.android.service.job.VideoEndJob;

import java.util.Date;

import rx.Observable;
import timber.log.Timber;

/**
 * @author lpthanh
 */
public class RecordingService {

    private static final int SEGMENT_DURATION = 3000;

    private final Context context;

    private final ApiService apiService;

    private final JobService jobService;

    private Video currentVideo;

    private VideoSegment currentSegment;

    public RecordingService(Context context,
                            ApiService apiService,
                            JobService jobService) {

        this.context = context;
        this.apiService = apiService;
        this.jobService = jobService;
    }

    public void setCurrent(Video currentVideo, VideoSegment currentSegment) {
        if (currentSegment != null && currentVideo != null &&
                (currentSegment.getVideoId() == null ||
                        !currentSegment.getVideoId().equals(currentVideo.getVideoId()))) {

            throw new IllegalArgumentException(String.format("Segment (%s) does not belong to video (%s).",
                    currentVideo, currentSegment));
        }

        this.currentVideo = currentVideo;
        this.currentSegment = currentSegment;
    }

    public boolean isBeingRecorded(long videoId) {
        if (currentVideo == null) {
            return false;
        }

        return currentVideo.getVideoId() == videoId;
    }

    /**
     * @return null if the recording of video ID is not ongoing.
     */
    public Long getCurrentOngoingSegmentId(long videoId) {
        if (!isBeingRecorded(videoId)) {
            return null;
        }

        if (currentSegment == null) {
            return null;
        }

        return currentSegment.getSegmentId();
    }

    public int getSegmentDuration() {
        return SEGMENT_DURATION;
    }

    public Observable<RecordingSession> createNewRecording(String title) {
        Video video = new Video();
        video.setTitle(title);
        video.setCreatedAt(new Date());
        video.setStatus(VideoStatus.EMPTY);
        video.setType(VideoType.LIVE);

        return apiService
                .createVideo(video)
                .map(RecordingSessionImpl::new);
    }

    private class RecordingSessionImpl extends RecordingSession {

        public RecordingSessionImpl(Video video) {
            super(context, RecordingService.this, video);
        }

        @Override
        protected void onRecordingStarted(Video video) {
            setCurrent(video, null);
            jobService.setUrgentMode(true);
        }

        @Override
        protected void onRecordingEnded(Video video, VideoSegment lastSegment) {
            setCurrent(null, null);

            // mark video ends
            long lastSegmentId;
            if (lastSegment == null) {
                lastSegmentId = 0;
                Timber.w("Last segment is null");
            } else {
                lastSegmentId = lastSegment.getSegmentId();
            }

            jobService.submitJob(new VideoEndJob(video.getVideoId(), lastSegmentId));
            jobService.setUrgentMode(false);
        }

        @Override
        protected void onSegmentRecorded(Video video, VideoSegment segment) {
            setCurrent(video, segment);

            // upload the segment
            jobService.submitUrgentJob(new SegmentLiveUploadJob(segment, getSegmentDuration()));
        }
    }

}

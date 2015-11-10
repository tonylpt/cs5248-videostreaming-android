package com.cs5248.android.service;

import android.content.Context;

import com.cs5248.android.model.Video;
import com.cs5248.android.model.VideoSegment;
import com.cs5248.android.model.VideoStatus;
import com.cs5248.android.model.VideoType;
import com.path.android.jobqueue.JobManager;

import java.util.Date;

import rx.Observable;

/**
 * @author lpthanh
 */
public class RecordingService {

    private final Context context;

    private final ApiService apiService;

    private final JobManager jobManager;

    private Video currentVideo;

    private VideoSegment currentSegment;

    public RecordingService(Context context, ApiService apiService, JobManager jobManager) {
        this.context = context;
        this.apiService = apiService;
        this.jobManager = jobManager;
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

    public Observable<Recording> createNewRecording(String title) {
        Video video = new Video();
        video.setTitle(title);
        video.setCreatedAt(new Date());
        video.setStatus(VideoStatus.EMPTY);
        video.setType(VideoType.LIVE);

        return apiService
                .createVideo(video)
                .map(RecordingImpl::new);
    }

    private class RecordingImpl extends Recording {

        public RecordingImpl(Video video) {
            super(context, RecordingService.this, video);
        }

        @Override
        protected void onRecordingStarted(Video video) {
            setCurrent(video, null);
        }

        @Override
        protected void onRecordingEnded(Video video) {
            setCurrent(null, null);
        }

        @Override
        protected void onSegmentRecorded(Video video, VideoSegment segment) {
            setCurrent(video, segment);

            // upload the segment
            jobManager.addJob(new SegmentLiveUploadJob(segment));
        }
    }

}

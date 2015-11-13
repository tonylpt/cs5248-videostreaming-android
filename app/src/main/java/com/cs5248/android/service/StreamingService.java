package com.cs5248.android.service;

import android.content.Context;
import android.net.Uri;

import com.cs5248.android.model.Video;
import com.cs5248.android.model.VideoSegment;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescription;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescriptionParser;

import java.io.InputStream;

import timber.log.Timber;

/**
 * @author lpthanh
 */
public class StreamingService {

    private final Context context;

    private final ApiService apiService;

    private final JobService jobService;

    private final MediaPresentationDescriptionParser mpdParser =
            new MediaPresentationDescriptionParser();

    private Video currentVideo;

    private VideoSegment currentSegment;

    public StreamingService(Context context,
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

    public boolean isBeingStreamed(long videoId) {
        if (currentVideo == null) {
            return false;
        }

        return currentVideo.getVideoId() == videoId;
    }

    /**
     * @return null if the recording of video ID is not ongoing.
     */
    public Long getCurrentOngoingSegmentId(long videoId) {
        if (!isBeingStreamed(videoId)) {
            return null;
        }

        if (currentSegment == null) {
            return null;
        }

        return currentSegment.getSegmentId();
    }

    public MediaPresentationDescription getMPD(Video video, Long lastSegmentId) {
        try {
//            Uri mpdUri = video.buildMPDUri();
//            if (mpdUri == null) {
//                Timber.w("MPD Uri is not available for video [%d]", video.getVideoId());
//                return null;
//            }

            InputStream mpdStream = apiService.streamMPD(video.getVideoId(), lastSegmentId);
            MediaPresentationDescription mpd = mpdParser.parse(video.getBaseUrl(), mpdStream);
            return mpd;
        } catch (Exception e) {
            Timber.e(e, "Error reading MPD stream");
            return null;
        }
    }

    public StreamingSession openSession(Video video) {
        return new StreamingSessionImpl(video);
    }

    private class StreamingSessionImpl extends StreamingSession {

        public StreamingSessionImpl(Video video) {
            super(context, StreamingService.this, video);
        }

        @Override
        protected void onStreamingStarted(Video video) {
            setCurrent(video, null);
            MediaPresentationDescription mpd = getMPD(video, null);
        }

        @Override
        protected void onStreamingEnded(Video video) {
            setCurrent(null, null);
        }

        @Override
        protected void onSegmentDownloaded(Video video, VideoSegment segment) {

        }

        @Override
        protected void onSegmentPlayed(Video video, VideoSegment segment) {

        }
    }

}

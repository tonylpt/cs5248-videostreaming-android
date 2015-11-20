package com.cs5248.android.service;

/**
 * @author lpthanh
 */
public class SegmentationException extends RuntimeException {

    private final long videoId;

    public SegmentationException(long videoId) {
        this.videoId = videoId;
    }

    public SegmentationException(String detailMessage, long videoId) {
        super(detailMessage);
        this.videoId = videoId;
    }

    public SegmentationException(String detailMessage, Throwable throwable, long videoId) {
        super(detailMessage, throwable);
        this.videoId = videoId;
    }

    public SegmentationException(Throwable throwable, long videoId) {
        super(throwable);
        this.videoId = videoId;
    }

    public long getVideoId() {
        return videoId;
    }

}

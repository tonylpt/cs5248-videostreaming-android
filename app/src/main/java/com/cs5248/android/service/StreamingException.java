package com.cs5248.android.service;

/**
 * @author lpthanh
 */
public class StreamingException extends RuntimeException {

    private final long videoId;

    public StreamingException(long videoId) {
        this.videoId = videoId;
    }

    public StreamingException(String detailMessage, long videoId) {
        super(detailMessage);
        this.videoId = videoId;
    }

    public StreamingException(String detailMessage, Throwable throwable, long videoId) {
        super(detailMessage, throwable);
        this.videoId = videoId;
    }

    public StreamingException(Throwable throwable, long videoId) {
        super(throwable);
        this.videoId = videoId;
    }

    public long getVideoId() {
        return videoId;
    }

}

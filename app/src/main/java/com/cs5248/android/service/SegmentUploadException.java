package com.cs5248.android.service;

import com.cs5248.android.model.VideoSegment;

/**
 * @author lpthanh
 */
public class SegmentUploadException extends RuntimeException {

    private final VideoSegment source;

    public SegmentUploadException(VideoSegment source) {
        this.source = source;
    }

    public SegmentUploadException(String message, VideoSegment source) {
        super(message);
        this.source = source;
    }

    public SegmentUploadException(String message, Throwable cause, VideoSegment source) {
        super(message, cause);
        this.source = source;
    }

    public SegmentUploadException(Throwable cause, VideoSegment source) {
        super(cause);
        this.source = source;
    }

    public VideoSegment getSource() {
        return source;
    }

}
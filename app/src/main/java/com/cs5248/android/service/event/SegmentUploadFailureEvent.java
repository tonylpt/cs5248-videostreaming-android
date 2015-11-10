package com.cs5248.android.service.event;

import com.cs5248.android.model.VideoSegment;

/**
 * Signifies that the segment has been successfully uploaded.
 *
 * @author lpthanh
 */
public class SegmentUploadFailureEvent {

    private final VideoSegment segment;

    private final Throwable error;

    public SegmentUploadFailureEvent(VideoSegment segment, Throwable error) {
        this.segment = segment;
        this.error = error;
    }

    public VideoSegment getSegment() {
        return segment;
    }

    public Throwable getError() {
        return error;
    }

}

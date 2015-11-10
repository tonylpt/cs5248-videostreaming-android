package com.cs5248.android.service.event;

import com.cs5248.android.model.VideoSegment;

/**
 * Signifies that the segment has been successfully uploaded.
 *
 * @author lpthanh
 */
public class SegmentUploadedEvent {

    private final VideoSegment segment;

    public SegmentUploadedEvent(VideoSegment segment) {
        this.segment = segment;
    }

    public VideoSegment getSegment() {
        return segment;
    }

}

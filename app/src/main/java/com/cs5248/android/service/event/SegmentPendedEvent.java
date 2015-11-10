package com.cs5248.android.service.event;

import com.cs5248.android.model.VideoSegment;

/**
 * Signifies that the segment has been added to the Pending queue to retry later.
 *
 * @author lpthanh
 */
public class SegmentPendedEvent {

    private final VideoSegment segment;

    public SegmentPendedEvent(VideoSegment segment) {
        this.segment = segment;
    }

    public VideoSegment getSegment() {
        return segment;
    }

}

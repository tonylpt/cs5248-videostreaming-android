package com.cs5248.android.service.job;

import com.cs5248.android.service.StreamingSession;

/**
 * Since all download jobs are not disk persistent, any of its operation can be invoked directly
 * on the passed in session object.
 * <p>
 * This job downloads one segment of the video.
 *
 * @author lpthanh
 */
public class SegmentDownloadJob extends DownloadJob {

    private final StreamingSession.StreamingSegment segment;

    public SegmentDownloadJob(StreamingSession session,
                              StreamingSession.StreamingSegment segment) {

        super(session);
        this.segment = segment;
    }

    @Override
    protected void performJob(StreamingSession session) {

        session._jobPerformDownloadSegment(segment);

    }

}

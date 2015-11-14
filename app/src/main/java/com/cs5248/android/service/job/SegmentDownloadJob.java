package com.cs5248.android.service.job;

import com.cs5248.android.service.StreamingSession;
import com.path.android.jobqueue.RetryConstraint;

/**
 * Since all download jobs are not disk persistent, any of its operation can be invoked directly
 * on the passed in session object.
 * <p>
 * This job downloads one segment of the video.
 *
 * @author lpthanh
 */
public class SegmentDownloadJob extends DownloadJob {

    private final StreamingSession.Streamlet segment;

    public SegmentDownloadJob(StreamingSession session,
                              StreamingSession.Streamlet segment) {

        super(session);
        this.segment = segment;
    }

    @Override
    protected void performJob(StreamingSession session) {

        session._jobPerformDownloadSegment(segment);

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable,
                                                     int runCount,
                                                     int maxRunCount) {
        return RetryConstraint.CANCEL;
    }
}

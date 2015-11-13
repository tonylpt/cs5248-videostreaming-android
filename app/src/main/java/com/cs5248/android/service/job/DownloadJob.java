package com.cs5248.android.service.job;

import com.cs5248.android.service.StreamingSession;
import com.path.android.jobqueue.RetryConstraint;

import java.util.Objects;

/**
 * A base class for all download jobs.
 * <p>
 * Since all download jobs are not disk persistent, any of its operation can be invoked directly
 * on the passed in session object.
 *
 * @author lpthanh
 */
public abstract class DownloadJob extends QueryJob {

    public static final String DOWNLOAD_JOB_GROUP_ID = "job.streaming.download";

    public static final String STREAMING_JOB_TAG = "STREAMING";

    private final StreamingSession session;

    public DownloadJob(StreamingSession session) {
        super(JobPriority.MID, 0, DOWNLOAD_JOB_GROUP_ID, STREAMING_JOB_TAG);

        Objects.requireNonNull(session);

        this.session = session;
    }

    protected abstract void performJob(StreamingSession session);

    @Override
    public void onRun() throws Throwable {
        if (!session.isProgressing()) {
            return;
        }

        performJob(session);
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable,
                                                     int runCount,
                                                     int maxRunCount) {
        if (session.isProgressing()) {
            return RetryConstraint.RETRY;
        } else {
            return RetryConstraint.CANCEL;
        }
    }
}

package com.cs5248.android.service.job;

import com.cs5248.android.service.RecordingSession;
import com.path.android.jobqueue.RetryConstraint;

import java.util.Objects;

import timber.log.Timber;

/**
 * This class is to split a streak into segment on the job queue.
 *
 * @author lpthanh
 */
public class StreakSegmentationJob extends QueryJob {

    public static final String SEGMENTATION_JOB_ID = "job.streak";

    private final RecordingSession.RecordingStreak streak;

    public StreakSegmentationJob(RecordingSession.RecordingStreak streak) {
        super(JobPriority.MID, 0, SEGMENTATION_JOB_ID);

        Objects.requireNonNull(streak);

        this.streak = streak;
    }

    @Override
    public void onRun() throws Throwable {
        Timber.d("Segmenting streak: %s", streak.getRecordFile().getAbsolutePath());
        streak._jobSegmentStreak();
    }

    private void cleanUp() {
        streak._jobCleanupStreakFile();
    }

    @Override
    protected void onCancel() {
        super.onCancel();
        cleanUp();
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable,
                                                     int runCount,
                                                     int maxRunCount) {

        // If it fails, it fails. Processing jobs can't be retried.
        return RetryConstraint.CANCEL;
    }
}

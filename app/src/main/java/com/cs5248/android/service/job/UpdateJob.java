package com.cs5248.android.service.job;

import com.path.android.jobqueue.Params;

import timber.log.Timber;

/**
 * The base for all jobs.
 *
 * @author lpthanh
 */
abstract class UpdateJob extends AppJob {

    public UpdateJob(int priority, int delayMillis, String groupId) {
        super(new Params(priority)
                        .requireNetwork()
                        .persist()
                        .delayInMs(delayMillis)
                        .groupBy(groupId)
        );
    }

    @Override
    public void onAdded() {
        // job has been secured to disk, add item to database
        Timber.d("Job has been persisted to disk. Job class=%s", getClass().getName());
    }

}

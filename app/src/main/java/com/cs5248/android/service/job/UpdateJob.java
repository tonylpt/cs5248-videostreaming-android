package com.cs5248.android.service.job;

import com.path.android.jobqueue.Params;

import timber.log.Timber;

/**
 * The base for all updating jobs (persisted on disk so it can be retried if app crashes).
 *
 * @author lpthanh
 */
abstract class UpdateJob extends JobBase {

    public UpdateJob(int priority, int delayMillis, String groupId, String... tags) {
        super(new Params(priority)
                        .requireNetwork()
                        .persist()
                        .delayInMs(delayMillis)
                        .groupBy(groupId)
                        .addTags(tags)
        );
    }

    @Override
    public void onAdded() {
        // job has been secured to disk, add item to database
        Timber.d("Job has been persisted to disk. Job class=%s", getClass().getName());
    }

}

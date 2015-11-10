package com.cs5248.android.service.job;

import com.cs5248.android.StreamingApplication;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.Params;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * The base for all jobs.
 *
 * @author lpthanh
 */
abstract class UsingNetworkJob extends Job {

    public UsingNetworkJob(int priority, int delayMillis, String groupId) {
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

    @Override
    protected void onCancel() {
        Timber.d("Job has been canceled. Job class=%s", getClass().getName());
    }

    protected final StreamingApplication getApplication() {
        return (StreamingApplication) getApplicationContext();
    }

    protected final void postEvent(Object event) {
        EventBus.getDefault().post(event);
    }

}

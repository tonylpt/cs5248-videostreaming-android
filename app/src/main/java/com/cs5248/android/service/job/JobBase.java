package com.cs5248.android.service.job;

import com.cs5248.android.StreamingApplication;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * The base for all jobs.
 *
 * @author lpthanh
 */
abstract class JobBase extends Job {

    protected JobBase(Params params) {
        super(params);
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

package com.cs5248.android.service.job;

import com.path.android.jobqueue.Params;

/**
 * The base for all jobs.
 *
 * @author lpthanh
 */
abstract class QueryJob extends AppJob {

    public QueryJob(int priority, int delayMillis, String groupId) {
        super(new Params(priority)
                        .requireNetwork()
                        .delayInMs(delayMillis)
                        .groupBy(groupId)
        );
    }

    @Override
    public void onAdded() {
    }

}

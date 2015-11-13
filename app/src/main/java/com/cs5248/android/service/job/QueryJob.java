package com.cs5248.android.service.job;

import com.path.android.jobqueue.Params;

/**
 * The base for all querying jobs (not persisted on disk).
 *
 * @author lpthanh
 */
abstract class QueryJob extends JobBase {

    public QueryJob(int priority, int delayMillis, String groupId, String... tags) {
        super(new Params(priority)
                        .requireNetwork()
                        .delayInMs(delayMillis)
                        .groupBy(groupId)
                        .addTags(tags)
        );
    }

    @Override
    public void onAdded() {
    }

}

package com.cs5248.android.service;

import android.content.Context;

import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.TagConstraint;
import com.path.android.jobqueue.config.Configuration;
import com.path.android.jobqueue.log.CustomLogger;

import timber.log.Timber;

/**
 * Encapsulate two separate job managers. One for urgent tasks and one for non-urgent tasks.
 *
 * @author lpthanh
 */
public class JobService {

    private final JobManager urgentJobManager;

    private final JobManager lowPriorityJobManager;

    public JobService(Context context) {
        Configuration urgentConfig = new Configuration.Builder(context)
                .id("urgentJobs")
                .customLogger(new TimberJobLogger())
                .minConsumerCount(1)
                .maxConsumerCount(2)
                .loadFactor(1)
                .consumerKeepAlive(120) // wait 2 minute
                .build();

        Configuration lowConfig = new Configuration.Builder(context)
                .id("lowPriorityJobs")
                .customLogger(new TimberJobLogger())
                .minConsumerCount(2)
                .maxConsumerCount(3)
                .loadFactor(2)
                .consumerKeepAlive(120) // wait 2 minute
                .build();

        this.urgentJobManager = new JobManager(context, urgentConfig);
        this.lowPriorityJobManager = new JobManager(context, lowConfig);
    }

    public void setUrgentMode(boolean urgent) {
        if (urgent) {
            lowPriorityJobManager.stop();
        } else {
            lowPriorityJobManager.start();
        }
    }

    public void submitUrgentJob(Job job) {
        urgentJobManager.addJob(job);
    }

    /**
     * Submit a normal, less prioritized job.
     */
    public void submitJob(Job job) {
        lowPriorityJobManager.addJob(job);
    }

    public void removeJobByTag(String tag) {
        lowPriorityJobManager.cancelJobs(TagConstraint.ANY, tag);
        urgentJobManager.cancelJobs(TagConstraint.ANY, tag);
    }

    /**
     * Configure job logging with Timber
     */
    private static class TimberJobLogger implements CustomLogger {

        @Override
        public boolean isDebugEnabled() {
            return false; // BuildConfig.DEBUG;
        }

        @Override
        public void d(String text, Object... args) {
            Timber.d(text, args);
        }

        @Override
        public void e(Throwable t, String text, Object... args) {
            Timber.e(t, text, args);
        }

        @Override
        public void e(String text, Object... args) {
            Timber.e(text, args);
        }
    }
}

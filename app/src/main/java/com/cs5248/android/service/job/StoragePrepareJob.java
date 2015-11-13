package com.cs5248.android.service.job;

import com.path.android.jobqueue.RetryConstraint;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Objects;

import timber.log.Timber;

/**
 * Cleans up the download storage directory when the app starts.
 *
 * @author lpthanh
 */
public class StoragePrepareJob extends QueryJob {

    static final String STREAMING_JOB_TAG = "STREAMING";

    private final File storageDir;

    public StoragePrepareJob(File storageDir) {
        // share the same group ID with the download jobs because we want this job to finish
        // before any stream can be downloaded
        super(JobPriority.MID, 0,
                DownloadJob.DOWNLOAD_JOB_GROUP_ID,
                STREAMING_JOB_TAG);

        Objects.requireNonNull(storageDir);

        this.storageDir = storageDir;
    }

    @Override
    public void onRun() throws Throwable {
        try {
            if (storageDir.exists() && storageDir.isDirectory()) {
                FileUtils.cleanDirectory(storageDir);
            }
        } catch (Exception e) {
            Timber.e(e, "Error while cleaning up the download directory");
        }
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable,
                                                     int runCount,
                                                     int maxRunCount) {
        return RetryConstraint.CANCEL;
    }
}

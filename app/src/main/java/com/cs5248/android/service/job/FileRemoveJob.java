package com.cs5248.android.service.job;

import com.path.android.jobqueue.RetryConstraint;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Objects;

import timber.log.Timber;

/**
 * Delete a file (eg. delete the segment file after it has been played back).
 *
 * @author lpthanh
 */
public class FileRemoveJob extends QueryJob {

    private final File file;

    public FileRemoveJob(File file, boolean waitForDownload) {
        super(JobPriority.LOW, 0,
                // use the same group ID as the download jobs so it waits for the download jobs
                // to finish before running.
                waitForDownload ? DownloadJob.DOWNLOAD_JOB_GROUP_ID : "job.cleanup"
        );

        Objects.requireNonNull(file);

        this.file = file;
    }

    @Override
    public void onRun() throws Throwable {
        try {
            if (!file.exists()) {
                return;
            }

            FileUtils.forceDelete(file);

        } catch (Exception e) {
            Timber.e(e, "Error while trying to delete '%s'", file.getAbsolutePath());
        }
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable,
                                                     int runCount,
                                                     int maxRunCount) {
        return RetryConstraint.CANCEL;
    }
}

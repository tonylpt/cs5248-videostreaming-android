package com.cs5248.android.service.job;

import com.cs5248.android.StreamingApplication;
import com.cs5248.android.model.Video;
import com.cs5248.android.service.StreamingService;
import com.cs5248.android.service.StreamingSession;
import com.path.android.jobqueue.RetryConstraint;

import java.util.Objects;

import timber.log.Timber;

/**
 * @author lpthanh
 */
public class MpdDownloadJob extends QueryJob {

    static final String MPD_DOWNLOAD_JOB_GROUP_ID = "job.mpd";

    private final StreamingSession session;

    public MpdDownloadJob(StreamingSession session) {
        super(JobPriority.MID, 0, MPD_DOWNLOAD_JOB_GROUP_ID);

        Objects.requireNonNull(session);

        this.session = session;
    }

    @Override
    public void onRun() throws Throwable {
        StreamingApplication application = getApplication();
        StreamingService streamingService = application.streamingService();

        try {
            Video result =

        } catch (Exception e) {
            Timber.e(e, "Failed to mark the video end. ID=%d", videoId);
            throw e;
        }
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable,
                                                     int runCount,
                                                     int maxRunCount) {
        // always retry
        return RetryConstraint.RETRY;
    }
}

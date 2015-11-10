package com.cs5248.android.service.job;

import com.cs5248.android.StreamingApplication;
import com.cs5248.android.model.Video;
import com.cs5248.android.service.ApiService;
import com.path.android.jobqueue.RetryConstraint;

import java.util.Objects;

import timber.log.Timber;

/**
 * @author lpthanh
 */
public class VideoEndJob extends UsingNetworkJob {

    private long videoId;

    private long lastSegmentId;

    public VideoEndJob(Long videoId, Long lastSegmentId) {
        // use the same group ID since this needs to come after any upload jobs
        super(JobPriority.MID, 0, SegmentUploadJob.UPLOAD_JOB_GROUP_ID);

        // validate
        Objects.requireNonNull(videoId);
        Objects.requireNonNull(lastSegmentId);

        this.videoId = videoId;
        this.lastSegmentId = lastSegmentId;
    }

    @Override
    public void onRun() throws Throwable {
        StreamingApplication application = getApplication();
        ApiService api = application.apiService();

        Timber.d("Marking video ending: id=%d.", videoId);

        try {
            Video result = api.markVideoUploadEnd(videoId, lastSegmentId);

            Timber.i("Video (%d)has been marked ended.", result.getVideoId());

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

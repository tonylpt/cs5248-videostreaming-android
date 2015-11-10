package com.cs5248.android.service.job;

import com.cs5248.android.StreamingApplication;
import com.cs5248.android.model.VideoSegment;
import com.cs5248.android.service.ApiService;
import com.cs5248.android.service.SegmentUploadException;
import com.cs5248.android.service.event.SegmentUploadFailureEvent;
import com.cs5248.android.service.event.SegmentUploadStartEvent;
import com.cs5248.android.service.event.SegmentUploadSuccessEvent;
import com.path.android.jobqueue.RetryConstraint;

import java.io.File;

import timber.log.Timber;

/**
 * The idea is that there are two types of segment upload jobs: Live and Pending.
 * Live upload job is the intermediate one. If it fails, it will not be retried but instead will
 * spawn another job into the Pending queue, with lower priority.
 * <p>
 * Also, if at the time the execution starts, and the segment is no longer the latest one recorded
 * by the camera, it will be skipped and pushed into the pending queue.
 * <p>
 * This way, the upload might not be in sequential order and it requires the server to be smart
 * about it, able to tolerate non-consecutive segment IDs.
 *
 * @author lpthanh
 */
public class SegmentPendingUploadJob extends SegmentUploadJob {

    public SegmentPendingUploadJob(VideoSegment segment, int segmentDuration) {
        // delay this job for longer than a segment duration just so it does not
        // have a chance to run while there are still incoming Live jobs
        // update: no need to delay since we have two job managers now.
        super(segment, segmentDuration, JobPriority.HIGH, 0, UPLOAD_JOB_GROUP_ID);
    }

    @Override
    public void onRun() throws Throwable {
        VideoSegment segment = getSegment();
        if (segment == null) {
            return;
        }

        StreamingApplication application = getApplication();
        ApiService service = application.apiService();

        try {
            Timber.d("Uploading pended segment %s", segment);
            postEvent(new SegmentUploadStartEvent(segment));

            VideoSegment result = service.uploadSegment(segment);

            // notify whoever interested in knowing the upload has succeeded
            postEvent(new SegmentUploadSuccessEvent(result));

            cleanUpSegmentFile();
        } catch (Exception e) {
            // do not retry and enqueue a new task into the Pending queue
            Timber.e("Failed to upload segment %d for video %d. To be retried again.",
                    segment.getVideoId(), segment.getSegmentId());

            postEvent(new SegmentUploadFailureEvent(segment, e));

            throw e;
        }
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable,
                                                     int runCount,
                                                     int maxRunCount) {

        if (throwable instanceof SegmentUploadException) {
            VideoSegment segment = ((SegmentUploadException) throwable).getSource();
            File file = new File(segment.getOriginalPath());

            if (!file.exists() || !file.isFile()) {
                Timber.e("Segment file has been deleted. Cancelling upload job.");
                return RetryConstraint.CANCEL;
            }
        }

        return RetryConstraint.RETRY;
    }
}

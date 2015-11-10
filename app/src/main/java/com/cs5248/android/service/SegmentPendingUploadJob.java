package com.cs5248.android.service;

import com.cs5248.android.StreamingApplication;
import com.cs5248.android.model.VideoSegment;
import com.cs5248.android.service.event.SegmentUploadedEvent;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import com.path.android.jobqueue.RetryConstraint;

import java.io.File;

import de.greenrobot.event.EventBus;
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
public class SegmentPendingUploadJob extends Job {

    public static final String PENDING_JOB_GROUP_ID = "pending_segment_upload";

    private VideoSegment segment;

    public SegmentPendingUploadJob(VideoSegment segment) {
        super(new Params(JobPriority.LOW)
                        .requireNetwork()
                        .persist()
                        .groupBy(PENDING_JOB_GROUP_ID)
                        .setDelayMs(1000)
        );

        this.segment = segment;
    }

    @Override
    public void onAdded() {
        // job has been secured to disk, add item to database
    }

    @Override
    public void onRun() throws Throwable {
        VideoSegment segment = this.segment;
        if (segment == null) {
            return;
        }

        StreamingApplication application = (StreamingApplication) getApplicationContext();
        StreamingService service = application.streamingService();

        try {
            VideoSegment result = service.uploadSegment(segment);

            // notify whoever interested in knowing the upload has succeeded
            EventBus.getDefault().post(new SegmentUploadedEvent(result));

        } catch (Exception e) {
            // do not retry and enqueue a new task into the Pending queue
            Timber.e("Failed to upload segment %d for video %d. To be retried again.",
                    segment.getVideoId(), segment.getSegmentId());
            throw e;
        }
    }

    @Override
    protected void onCancel() {
        // delete the local file
        String originalFile = segment.getOriginalPath();
        if (originalFile == null) {
            return;
        }

        File file = new File(originalFile);
        if (!file.exists() || !file.isFile()) {
            return;
        }

        if (!file.delete()) {
            Timber.e("Failed to delete file: %s", file.getAbsolutePath());
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

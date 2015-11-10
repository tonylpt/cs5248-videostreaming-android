package com.cs5248.android.service;

import com.cs5248.android.StreamingApplication;
import com.cs5248.android.model.VideoSegment;
import com.cs5248.android.service.event.SegmentPendedEvent;
import com.cs5248.android.service.event.SegmentUploadedEvent;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.JobManager;
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
public class SegmentLiveUploadJob extends Job {

    public static final String LIVE_JOB_GROUP_ID = "live_segment_upload";

    /**
     * If a segment is too far behind the latest segment, it will be pended.
     */
    private static final long LIVE_THRESHOLD = 2;

    private VideoSegment segment;

    public SegmentLiveUploadJob(VideoSegment segment) {
        super(new Params(JobPriority.HIGH)
                        .requireNetwork()
                        .persist()
                        .groupBy(LIVE_JOB_GROUP_ID)
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
        RecordingService recordingService = application.recordingService();
        JobManager jobManager = application.jobManager();

        boolean shouldAddPending = false;

        // check if this segment upload should be delayed
        if (!recordingService.isBeingRecorded(segment.getVideoId())) {
            shouldAddPending = true;
        }

        if (!shouldAddPending) {
            Long onGoingId = recordingService.getCurrentOngoingSegmentId(segment.getVideoId());
            if (onGoingId == null) {
                shouldAddPending = true;
            } else {
                long gapToLatestSegment = onGoingId - segment.getSegmentId();
                if (gapToLatestSegment > LIVE_THRESHOLD) {
                    // if this segment is too far behind the latest one
                    shouldAddPending = true;
                }
            }
        }

        if (!shouldAddPending) {
            try {
                VideoSegment result = service.uploadSegment(segment);

                // notify whoever interested in knowing the upload has succeeded
                EventBus.getDefault().post(new SegmentUploadedEvent(result));

            } catch (Exception e) {
                // do not retry and enqueue a new task into the Pending queue
                Timber.e("Failed to upload segment %d for video %d. Adding it to pending queue.",
                        segment.getVideoId(), segment.getSegmentId());
                shouldAddPending = true;
            }
        }

        if (shouldAddPending) {
            SegmentPendingUploadJob pendingUploadJob = new SegmentPendingUploadJob(segment);
            jobManager.addJob(pendingUploadJob);

            EventBus.getDefault().post(new SegmentPendedEvent(segment));
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

        // Live upload job is never retried right away in order to prioritize newer segments.
        // Another Pending upload job must have been added to the Pending queue already.
        return RetryConstraint.CANCEL;
    }
}

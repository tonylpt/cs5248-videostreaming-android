package com.cs5248.android.service.job;

import com.cs5248.android.StreamingApplication;
import com.cs5248.android.model.VideoSegment;
import com.cs5248.android.service.ApiService;
import com.cs5248.android.service.JobService;
import com.cs5248.android.service.RecordingService;
import com.cs5248.android.service.event.SegmentUploadFailureEvent;
import com.cs5248.android.service.event.SegmentUploadPendedEvent;
import com.cs5248.android.service.event.SegmentUploadStartEvent;
import com.cs5248.android.service.event.SegmentUploadSuccessEvent;
import com.path.android.jobqueue.RetryConstraint;

import java.io.IOException;

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
public class SegmentLiveUploadJob extends SegmentUploadJob {

    /**
     * If a segment is too far behind the latest segment, it will be pended.
     */
    private static final long LIVE_THRESHOLD = 2;

    public SegmentLiveUploadJob(VideoSegment segment, int segmentDuration) {
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
        RecordingService recordingService = application.recordingService();
        JobService jobService = application.jobService();

        boolean shouldAddPending = false;

        // check if this segment upload should be delayed
        // if the recording is still going on, and the current segment is too far behind
        // the latest one, add it to the Pending queue.
        // if the recording is over, and this segment is still in the Live queue, means that
        // this is the last few segments of a recent recording session, should be sent right now.
        Long onGoingId = recordingService.getCurrentOngoingSegmentId(segment.getVideoId());
        if (onGoingId != null) {
            long gapToLatestSegment = onGoingId - segment.getSegmentId();
            if (gapToLatestSegment > LIVE_THRESHOLD) {
                // if this segment is too far behind the latest one
                shouldAddPending = true;
            }
        }

        if (!shouldAddPending) {
            try {
                Timber.d("Uploading live segment %s", segment);
                postEvent(new SegmentUploadStartEvent(segment));

                /*
                // this is for testing to simulate failed segments
                if (segment.getSegmentId() % 3 == 0) {
                    Timber.d("Faking a fail segment id = %d", segment.getSegmentId());
                    throw new IOException("Fake error");
                }
                */

                VideoSegment result = service.uploadSegment(segment);

                // notify whoever interested in knowing the upload has succeeded
                postEvent(new SegmentUploadSuccessEvent(result));

                cleanUpSegmentFile();
            } catch (Exception e) {
                // do not retry and enqueue a new task into the Pending queue
                Timber.e("Failed to upload segment %d for video %d. Adding it to pending queue.",
                        segment.getVideoId(), segment.getSegmentId());
                shouldAddPending = true;

                postEvent(new SegmentUploadFailureEvent(segment, e));
            }
        }

        if (shouldAddPending) {
            SegmentPendingUploadJob pendingUploadJob = new SegmentPendingUploadJob(segment, getSegmentDuration());
            jobService.submitJob(pendingUploadJob);

            postEvent(new SegmentUploadPendedEvent(segment));
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

package com.cs5248.android.service.job;

import com.cs5248.android.model.VideoSegment;

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
abstract class SegmentUploadJob extends UpdateJob {

    static final String UPLOAD_JOB_GROUP_ID = "job.segments";

    static final String RECORDING_JOB_TAG = "RECORDING";

    private VideoSegment segment;

    private int segmentDuration;

    public SegmentUploadJob(VideoSegment segment,
                            int segmentDuration,
                            int priority,
                            int delayMillis,
                            String groupId) {

        super(priority, delayMillis, groupId, RECORDING_JOB_TAG);
        this.segment = segment;
        this.segmentDuration = segmentDuration;
    }

    public VideoSegment getSegment() {
        return segment;
    }

    public int getSegmentDuration() {
        return segmentDuration;
    }

    protected final void cleanUpSegmentFile() {
        // delete the local file
        String originalFile = getSegment().getOriginalPath();
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
    protected void onCancel() {
        super.onCancel();
        cleanUpSegmentFile();
    }

}

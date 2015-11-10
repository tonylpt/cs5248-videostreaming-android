package com.cs5248.android.service;

import com.cs5248.android.StreamingApplication;
import com.cs5248.android.model.VideoSegment;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

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
abstract class SegmentUploadJob extends Job {

    private VideoSegment segment;

    public SegmentUploadJob(VideoSegment segment, int priority, int delayMillis, String groupId) {
        super(new Params(priority)
                        .requireNetwork()
                        .persist()
                        .delayInMs(delayMillis)
                        .groupBy(groupId)
        );

        this.segment = segment;
    }

    public VideoSegment getSegment() {
        return segment;
    }

    @Override
    public void onAdded() {
        // job has been secured to disk, add item to database
        Timber.d("Upload job has been persisted to disk. Job class=%s", getClass().getName());
    }

    protected StreamingApplication getApplication() {
        return (StreamingApplication) getApplicationContext();
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

    protected final void postEvent(Object event) {
        EventBus.getDefault().post(event);
    }

    @Override
    protected void onCancel() {
        cleanUpSegmentFile();
    }

}

package com.cs5248.android.service;

import android.content.Context;

/**
 * @author lpthanh
 */
public class RecordingService {

    private final Context context;

    public RecordingService(Context context) {
        this.context = context;
    }

    public boolean isBeingRecorded(long videoId) {
        // todo implement this
        return true;
    }

    /**
     * @return null if the recording of video ID is not ongoing.
     */
    public Long getCurrentOngoingSegmentId(long videoId) {
        if (!isBeingRecorded(videoId)) {
            return null;
        }

        // todo implement this
        return 0L;
    }

}

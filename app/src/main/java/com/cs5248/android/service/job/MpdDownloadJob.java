package com.cs5248.android.service.job;

import com.cs5248.android.service.StreamingSession;

/**
 * This job downloads the MPD of the video, which is to be run periodically during live streaming,
 * or in the beginning of a VOD streaming.
 *
 * @author lpthanh
 */
public class MpdDownloadJob extends DownloadJob {

    public MpdDownloadJob(StreamingSession session) {
        super(session);
    }

    @Override
    protected void performJob(StreamingSession session) {

        session._jobPerformUpdateMpd();

    }

}

package com.cs5248.android.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBar;

import com.cs5248.android.model.Video;
import com.cs5248.android.service.StreamingService;
import com.cs5248.android.service.StreamingSession;
import com.cs5248.android.util.BaseActivity;
import com.cs5248.android.util.Util;

import javax.inject.Inject;

import timber.log.Timber;

abstract class StreamingActivity extends BaseActivity {

    @Inject
    StreamingService streamingService;

    @Override
    protected void initActivity(Bundle savedInstanceState) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Video video = Util.getParcelable(this, "video", Video.class);
        if (video != null) {
            StreamingSession session = streamingService.openSession(video);
            if (session != null) {
                startStreaming();
            }
        } else {
            Timber.e("Could not find a video parcelable for this activity");
        }
    }

    private void startStreaming() {

    }

}

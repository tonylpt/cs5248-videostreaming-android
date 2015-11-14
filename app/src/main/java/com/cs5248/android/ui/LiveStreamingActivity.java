package com.cs5248.android.ui;

import com.cs5248.android.R;
import com.cs5248.android.dagger.ApplicationComponent;

public class LiveStreamingActivity extends StreamingActivity {

    /* some other custom things for live streaming */


    @Override
    protected void injectActivity(ApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_live_streaming;
    }

    @Override
    protected boolean isLiveStreaming() {
        return true;
    }

}

package com.cs5248.android.ui;

import android.widget.RelativeLayout;

import com.cs5248.android.R;
import com.cs5248.android.dagger.ApplicationComponent;

import butterknife.Bind;

/**
 * The activity for viewing a live stream.
 */
public class LiveStreamingActivity extends StreamingActivity {

    /* some other custom things for live streaming */

    @Bind(R.id.player_container_live)
    RelativeLayout playerContainer;

    public RelativeLayout getPlayerContainer() {
        return playerContainer;
    }


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

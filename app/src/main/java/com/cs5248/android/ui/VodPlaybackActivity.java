package com.cs5248.android.ui;

import android.widget.RelativeLayout;

import com.cs5248.android.R;
import com.cs5248.android.dagger.ApplicationComponent;

import butterknife.Bind;

public class VodPlaybackActivity extends StreamingActivity {

    /* some other custom things for VOD streaming */


    @Bind(R.id.player_container_vod)
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
        return R.layout.activity_vod_playback;
    }

    @Override
    protected boolean isLiveStreaming() {
        return false;
    }

}

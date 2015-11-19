package com.cs5248.android.ui;

import com.cs5248.android.R;
import com.cs5248.android.dagger.ApplicationComponent;

/**
 * The activity for viewing an on-demand video.
 */
public class VodPlaybackActivity extends StreamingActivity2 {

    /* some other custom things for VOD streaming */

    /*

    @Bind(R.id.player_container_vod)
    RelativeLayout playerContainer;

    public RelativeLayout getPlayerContainer() {
        return playerContainer;
    }
    */


    @Override
    protected void injectActivity(ApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_video_play;
    }

    @Override
    protected boolean isLiveStreaming() {
        return false;
    }

}

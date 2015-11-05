package com.cs5248.android.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.widget.Button;

import com.cs5248.android.R;
import com.cs5248.android.dagger.ApplicationComponent;
import com.cs5248.android.service.StreamingService;
import com.cs5248.android.util.BaseActivity;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.OnClick;

public class VodPlaybackActivity extends BaseActivity {

    @Inject
    StreamingService streamingService;

    @Bind(R.id.play_pause_button)
    Button playPauseButton;

    @Override
    protected void initActivity(Bundle savedInstanceState) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // other stuff
    }

    @OnClick(R.id.play_pause_button)
    public void onPlayPauseClick() {

    }

    @Override
    protected void injectActivity(ApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_vod_playback;
    }
}

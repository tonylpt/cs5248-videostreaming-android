package com.cs5248.android.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.widget.Button;
import android.widget.TextView;

import com.cs5248.android.R;
import com.cs5248.android.dagger.ApplicationComponent;
import com.cs5248.android.model.Video;
import com.cs5248.android.service.StreamingService;
import com.cs5248.android.util.BaseActivity;
import com.cs5248.android.util.Util;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.OnClick;
import timber.log.Timber;

public class VodPlaybackActivity extends BaseActivity {

    @Inject
    StreamingService streamingService;

    @Bind(R.id.play_pause_button)
    Button playPauseButton;

    @Bind(R.id.title_text)
    TextView titleText;

    @Override
    protected void initActivity(Bundle savedInstanceState) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Video video = Util.getParcelable(this, "video", Video.class);
        if (video != null) {
            display(video);
        } else {
            Timber.e("Could not find a video parcelable for this activity");
        }
    }

    private void display(Video video) {
        titleText.setText(video.getTitle());
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

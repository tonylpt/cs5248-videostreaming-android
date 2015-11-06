package com.cs5248.android.ui;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.widget.TextView;

import com.cs5248.android.R;
import com.cs5248.android.dagger.ApplicationComponent;
import com.cs5248.android.model.Video;
import com.cs5248.android.service.StreamingService;
import com.cs5248.android.util.BaseActivity;
import com.cs5248.android.util.Util;

import javax.inject.Inject;

import butterknife.Bind;

public class LiveStreamingActivity extends BaseActivity {

    @Inject
    StreamingService streamingService;

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
        }
    }

    private void display(Video video) {
        titleText.setText(video.getTitle());
    }

    @Override
    protected void injectActivity(ApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_live_streaming;
    }
}

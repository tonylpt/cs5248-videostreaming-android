package com.cs5248.android.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBar;

import com.cs5248.android.R;
import com.cs5248.android.dagger.ApplicationComponent;
import com.cs5248.android.service.StreamingService;
import com.cs5248.android.util.BaseActivity;

import javax.inject.Inject;

public class LiveStreamingActivity extends BaseActivity {

    @Inject
    StreamingService streamingService;

    @Override
    protected void initActivity(Bundle savedInstanceState) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // other stuff
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

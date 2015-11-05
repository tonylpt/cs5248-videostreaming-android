package com.cs5248.android.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBar;

import com.ToxicBakery.viewpager.transforms.RotateUpTransformer;
import com.cs5248.android.R;
import com.cs5248.android.dagger.ApplicationComponent;
import com.cs5248.android.service.Recording;
import com.cs5248.android.service.StreamingService;
import com.cs5248.android.util.BaseActivity;
import com.cs5248.android.util.WizardView;

import javax.inject.Inject;

import butterknife.Bind;

public class RecordActivity extends BaseActivity {

    @Inject
    StreamingService streamingService;

    @Bind(R.id.wizard_view)
    WizardView<Recording> wizardView;

    @Override
    protected void initActivity(Bundle savedInstanceState) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        RecordStep1 step1 = new RecordStep1();
        RecordStep2 step2 = new RecordStep2();

        wizardView.setSteps(step1, step2);
        wizardView.setOnFinishHandler(ignored -> this.finish());
        wizardView.setPageTransformer(true, new RotateUpTransformer());
        wizardView.setTransitionDuration(1000);
    }

    @Override
    protected void injectActivity(ApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_record;
    }
}

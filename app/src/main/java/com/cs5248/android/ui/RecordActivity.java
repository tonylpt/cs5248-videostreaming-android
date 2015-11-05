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

    private RecordStep1 step1 = new RecordStep1();

    private RecordStep2 step2 = new RecordStep2();

    @Override
    protected void initActivity(Bundle savedInstanceState) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        wizardView.setSteps(step1, step2);
        wizardView.setOnFinishHandler(this::onFinish);
        wizardView.setPageTransformer(true, new RotateUpTransformer());
        wizardView.setTransitionDuration(1000);
    }

    private void onFinish(Recording recording) {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onBackPressed() {
        // if BACK is pressed at step 1, consider the operation cancelled
        setResult(step1.isVideoCreated() ? RESULT_OK : RESULT_CANCELED);
        finish();
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

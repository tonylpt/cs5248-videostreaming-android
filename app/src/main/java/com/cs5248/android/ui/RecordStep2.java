package com.cs5248.android.ui;

import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;

import com.cs5248.android.R;
import com.cs5248.android.dagger.ApplicationComponent;
import com.cs5248.android.service.CameraService;
import com.cs5248.android.service.Recording;
import com.cs5248.android.service.StreamingService;
import com.cs5248.android.util.WizardStep;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * @author lpthanh
 */
public class RecordStep2 extends WizardStep<Recording> {

    @Inject
    StreamingService streamingService;

    Recording currentRecording;

    @Override
    public void initView(View view, Bundle savedInstanceState) {
    }

    @OnClick(R.id.record_button)
    public void record(){
        currentRecording.startUploading();
        currentRecording.startRecording();
    }

    @OnClick(R.id.next_button)
    public void next() {
        finishStep(currentRecording);
    }

    @Override
    protected void startStep(Recording lastResult) {
        currentRecording = lastResult;

    }

    @Override
    protected void injectFragment(ApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_record_step_2;
    }
}

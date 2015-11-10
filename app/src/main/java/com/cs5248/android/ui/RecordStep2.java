package com.cs5248.android.ui;

import android.os.Bundle;
import android.view.View;

import com.cs5248.android.R;
import com.cs5248.android.dagger.ApplicationComponent;
import com.cs5248.android.service.CameraService;
import com.cs5248.android.service.Recording;
import com.cs5248.android.util.WizardStep;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * @author lpthanh
 */
public class RecordStep2 extends WizardStep<Recording> {

    @Inject
    CameraService cameraService;

    @Bind(R.id.camera_previewer)
    CameraPreviewer cameraPreviewer;

    private Recording currentRecording;

    private RecordingState currentState;

    @Override
    public void initView(View view, Bundle savedInstanceState) {
        cameraPreviewer.init(cameraService);
    }

    @OnClick(R.id.record_button)
    public void onRecordClicked() {
        switch (currentState) {
            case NOT_STARTED:
                startRecording();
                break;

            case PROGRESSING:
                endRecording();
                break;

            case ENDED:
                // not supposed to be in this state
                break;
        }
    }

    @Override
    protected void startStep(Recording lastResult) {
        currentRecording = lastResult;
        currentRecording.setPreviewer(cameraPreviewer);
        cameraPreviewer.start();
    }

    private void startRecording() {
        if(currentState != RecordingState.NOT_STARTED) {
            return;
        }

        currentState = RecordingState.PROGRESSING;
        currentRecording.startRecording();
    }

    /**
     * End the current recording (the preview may still be going on).
     */
    private void endRecording() {
        if (currentState != RecordingState.PROGRESSING) {
            return;
        }

        currentState = RecordingState.ENDED;
        currentRecording.endRecording();
        displaySummaryScreen();
    }

    /**
     * Stop the camera previewer.
     */
    private void endPreviewer() {
        cameraPreviewer.stop();
    }

    private void displaySummaryScreen() {
        endPreviewer();

        // just quit for now
        finishStep(currentRecording);
    }

    @Override
    public void onPause() {
        // end the recording when the activity is paused, if it's still going on
        endRecording();
        super.onPause();
    }

    @Override
    protected void injectFragment(ApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_record_step_2;
    }

    /**
     * To store the current state of the recorder.
     */
    private enum RecordingState {

        NOT_STARTED,

        PROGRESSING,

        ENDED
    }
}

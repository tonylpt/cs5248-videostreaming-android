package com.cs5248.android.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.cs5248.android.R;
import com.cs5248.android.dagger.ApplicationComponent;
import com.cs5248.android.service.Recording;
import com.cs5248.android.service.RecordingState;
import com.cs5248.android.util.WizardStep;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * @author lpthanh
 */
public class RecordStep2 extends WizardStep<Recording> {

//    @Inject
//    CameraService cameraService;
//
//    @Bind(R.id.camera_previewer)
//    CameraPreviewer cameraPreviewer;

    private Recording recording;

    @Bind(R.id.record_button)
    Button recordButton;

    @Override
    public void initView(View view, Bundle savedInstanceState) {
//        cameraPreviewer.init(cameraService);
    }

    @OnClick(R.id.record_button)
    public void onRecordClicked() {
        switch (recording.getRecordingState()) {
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
        recording = lastResult;
        recording.setStateChangeListener(new RecordingStateChangeListener());

//        recording.setPreviewer(cameraPreviewer);
//        cameraPreviewer.start();
    }

    private void startRecording() {
        recording.startRecording();
    }

    /**
     * End the current recording (the preview may still be going on).
     */
    private void endRecording() {
        recording.endRecording();
        displaySummaryScreen();
    }

    /**
     * Stop the camera previewer.
     */
    private void endPreviewer() {
//        cameraPreviewer.stop();
    }

    private void displaySummaryScreen() {
        endPreviewer();

        // just quit for now
        finishStep(recording);
    }

    @Override
    public void onPause() {
        // end the recording when the activity is paused, if it's still going on
        Recording recording = this.recording;
        if (recording != null && recording.isProgressing()) {
            endRecording();
        }

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

    private class RecordingStateChangeListener implements Recording.StateChangeListener {

        @Override
        public void stateChanged(RecordingState newState) {
            getActivity().runOnUiThread(() -> {
                recordButton.setText("State: " + newState);
            });
        }
    }
}

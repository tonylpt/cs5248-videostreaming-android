package com.cs5248.android.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.cs5248.android.R;
import com.cs5248.android.dagger.ApplicationComponent;
import com.cs5248.android.service.RecordingSession;
import com.cs5248.android.service.RecordingState;
import com.cs5248.android.util.WizardStep;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * @author lpthanh
 */
public class RecordStep2 extends WizardStep<RecordingSession> {

//    @Inject
//    CameraService cameraService;
//
//    @Bind(R.id.camera_previewer)
//    CameraPreviewer cameraPreviewer;

    private RecordingSession recordingSession;

    @Bind(R.id.record_button)
    Button recordButton;

    @Override
    public void initView(View view, Bundle savedInstanceState) {
//        cameraPreviewer.init(cameraService);
    }

    @OnClick(R.id.record_button)
    public void onRecordClicked() {
        switch (recordingSession.getRecordingState()) {
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
    protected void startStep(RecordingSession lastResult) {
        recordingSession = lastResult;
        recordingSession.setStateChangeListener(new RecordingStateChangeListener());

//        recording.setPreviewer(cameraPreviewer);
//        cameraPreviewer.start();
    }

    private void startRecording() {
        recordingSession.startRecording();
    }

    /**
     * End the current recording (the preview may still be going on).
     */
    private void endRecording() {
        recordingSession.endRecording();
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
        finishStep(recordingSession);
    }

    @Override
    public void onPause() {
        // end the recording when the activity is paused, if it's still going on
        RecordingSession recordingSession = this.recordingSession;
        if (recordingSession != null && recordingSession.isProgressing()) {
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

    private class RecordingStateChangeListener implements RecordingSession.StateChangeListener {

        @Override
        public void stateChanged(RecordingState newState) {
            getActivity().runOnUiThread(() -> {
                recordButton.setText("State: " + newState);
            });
        }
    }
}

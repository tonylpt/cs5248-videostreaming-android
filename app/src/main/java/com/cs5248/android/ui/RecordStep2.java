package com.cs5248.android.ui;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import com.cs5248.android.R;
import com.cs5248.android.dagger.ApplicationComponent;
import com.cs5248.android.service.RecordingSession;
import com.cs5248.android.service.RecordingState;
import com.cs5248.android.util.CameraHelper;
import com.cs5248.android.util.WizardStep;

import java.io.IOException;

import butterknife.Bind;
import butterknife.OnClick;
import timber.log.Timber;

/**
 * The second step of the recording process, that performs the actual video recording and uploading.
 *
 * @author lpthanh
 */
public class RecordStep2 extends WizardStep<RecordingSession> {

    public static final int PREVIEW_WIDTH = 1080;

    public static final int PREVIEW_HEIGHT = 720;

    public static final int SEGMENT_DURATION = 3000;

    public static final int SEGMENTS_PER_STREAK = 1;


    @Bind(R.id.camera_previewer)
    TextureView cameraPreviewer;

    @Bind(R.id.record_button)
    Button recordButton;

    private RecordingSession recordingSession;

    private MediaRecorder recorder;

    private Camera camera;

    private CamcorderProfile camcorderProfile;

    @Override
    public void initView(View view, Bundle savedInstanceState) {
        recordButton.setVisibility(View.INVISIBLE);
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

        // set up the camera
        recordingSession.runOnCameraThread(this::initCameraRecorder);
    }

    private void startRecording() {
        if (camera == null || recorder == null) {
            throw new RuntimeException("Not initialized");
        }

        recordingSession.startRecording(camera, camcorderProfile, recorder,
                SEGMENT_DURATION, SEGMENTS_PER_STREAK);
    }

    /**
     * End the current recording (the preview may still be going on).
     */
    private void endRecording() {
        recordingSession.endRecording();
    }

    /**
     * Stop the camera previewer.
     */
    private void endPreviewer() {
    }

    private void displaySummaryScreen() {
        endPreviewer();

        // just quit for now. Wait a little for the camera to finalize itself.
        runDelayed(() -> finishStep(recordingSession), 500);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (recordingSession == null) {
            return;
        }

        if (recordingSession.isProgressing()) {
            endRecording();
        }

        releaseCamera();
        recordingSession.dispose();
    }

    private void initCameraRecorder() {
        camera = CameraHelper.getDefaultCamera(getActivity());

        CamcorderProfile profile = this.camcorderProfile =
                CamcorderProfile.get(CamcorderProfile.QUALITY_480P);

        CameraHelper.setPreviewSizeAndOrientation(camera, profile,
                PREVIEW_WIDTH, PREVIEW_HEIGHT);

        try {
            camera.setPreviewTexture(cameraPreviewer.getSurfaceTexture());
            camera.startPreview();

            recorder = new MediaRecorder();
        } catch (IOException e) {
            Timber.e(e, "Surface texture cannot be used for preview");
        }

        // update UI
        runOnUiThread(() -> recordButton.setVisibility(View.VISIBLE));
    }

    private void releaseCamera() {
        final Camera camera = this.camera;
        this.camera = null;

        if (camera != null) {
            recordingSession.runOnCameraThread(() -> {
                camera.stopPreview();
                camera.release();
            });
        }
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
            switch (newState) {
                case PROGRESSING:
                    runOnUiThread(() -> recordButton.setText("End"));
                    break;
                case ENDED:
                    runOnUiThread(RecordStep2.this::displaySummaryScreen);
                    break;
            }
        }
    }

}

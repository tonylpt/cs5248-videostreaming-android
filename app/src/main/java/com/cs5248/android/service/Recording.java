package com.cs5248.android.service;

import android.graphics.ImageFormat;
import android.hardware.Camera;

import com.cs5248.android.ui.CameraPreviewer;

/**
 * @author lpthanh
 */
public abstract class Recording {

    public abstract void startRecording();

    public abstract void stopRecording();

    public abstract void startUploading();

    public void setPreviewer(CameraPreviewer previewer) {

    }

    public Camera initCamera(int frameWidth, int frameHeight, int fps) {
        Camera camera = null;
        Camera.CameraInfo info = new Camera.CameraInfo();
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i <
                numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                camera = Camera.open(i);
                break;
            }
        }
        if (camera == null) {
            camera = Camera.open();
        }
        if (camera == null) {
            throw new RuntimeException("Unable to open camera");
        }

        Camera.Parameters parms = camera.getParameters();
        parms.setPreviewFormat(ImageFormat.YV12);
        parms.setPreviewSize(frameWidth, frameHeight);
        parms.setPreviewFrameRate(fps);
        camera.setParameters(parms);

        return camera;
    }

    public void endRecording() {

    }
}

package com.cs5248.android.ui;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.cs5248.android.service.CameraService;

import java.io.IOException;

/**
 * @author lpthanh
 */
public class CameraPreviewer extends SurfaceView {

    private boolean previewIsRunning;

    private CameraService cameraService;

    private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraPreviewer(Context context) {
        super(context);
    }

    public CameraPreviewer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void init(CameraService cameraService) {
        this.cameraService = cameraService;
        SurfaceHolder holder = getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//        holder.addCallback(new PreviewerSurfaceCallback());
    }

    /**
     * Start the camera previewer (bind this to the camera)
     */
    public void start() {
//        mCamera = initCamera(frameWidth, frameHeight, fps);
    }

    /**
     * Stop the previewer (the recording should have been already stopped)
     */
    void stop() {

    }

//    void pausePreview() {
//        if (previewIsRunning && (camera != null)) {
//            camera.stopPreview();
//            previewIsRunning = false;
//        }
//    }
//
//    void resumePreview() {
//        if (!previewIsRunning && (camera != null)) {
//            camera.startPreview();
//            previewIsRunning = true;
//        }
//    }
//
//    public void startRecording() {
//        mCamera.startPreview();
//    }
//
//    public void stopRecording() {
//        mCamera.stopPreview();
//    }
//
//    public Camera initCamera(int frameWidth, int frameHeight, int fps) {
//        Camera camera = null;
//        Camera.CameraInfo info = new Camera.CameraInfo();
//        int numCameras = Camera.getNumberOfCameras();
//        for (int i = 0; i <
//                numCameras; i++) {
//            Camera.getCameraInfo(i, info);
//            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
//                camera = Camera.open(i);
//                break;
//            }
//        }
//        if (camera == null) {
//            camera = Camera.open();
//        }
//        if (camera == null) {
//            throw new RuntimeException("Unable to open camera");
//        }
//
//        Camera.Parameters parms = camera.getParameters();
//        parms.setPreviewFormat(ImageFormat.YV12);
//        parms.setPreviewSize(frameWidth, frameHeight);
//        parms.setPreviewFrameRate(fps);
//        camera.setParameters(parms);
//
//        return camera;
//    }
//
//
//    public void surfaceCreated(SurfaceHolder holder) {
//        try {
//
//            mCamera.setPreviewDisplay(holder);
//
//            camera = Camera.open()
//
//
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void surfaceDestroyed(SurfaceHolder holder) {
//        myStopPreview();
//        camera.release();
//        camera = null;
//    }
//
//    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
//        // set preview size etc here ... then
//        myStartPreview();
//
//
//
//
//
//
//        if (mHolder.getSurface() == null) {
//            return;
//        }
//        // stop preview before making changes
//        try {
//            //mCamera.stopPreview();
//        } catch (Exception e) {
//            e.printStackTrace();
//            // ignore: tried to stop a non-existent preview
//        }
//
//        try {
//            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
//                long lastMark = SystemClock.elapsedRealtime();
//                long counter = 0;
//
//                @Override
//                public void onPreviewFrame(byte[] data, Camera camera) {
//
//                    receiveCameraFrameCallback(data);
//                }
//            });
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private class PreviewerSurfaceCallback implements SurfaceHolder.Callback {
//
//        @Override
//        public void surfaceCreated(SurfaceHolder holder) {
//
//        }
//
//        @Override
//        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//
//        }
//
//        @Override
//        public void surfaceDestroyed(SurfaceHolder holder) {
//
//        }
//    }
}
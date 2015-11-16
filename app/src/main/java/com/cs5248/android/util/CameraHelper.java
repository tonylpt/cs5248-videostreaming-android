/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cs5248.android.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.os.Build;
import android.view.Surface;

import java.util.List;

import timber.log.Timber;

/**
 * Adapted from CameraHelper class in MediaRecorder sample project.
 */
public class CameraHelper {

    /**
     * Find the preview size that best fit the prescribed one.
     */
    public static Camera.Size getOptimalPreviewSize(Camera.Parameters params, int w, int h) {
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();

        // Use a very small tolerance because we want an exact match.
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null)
            return null;

        Camera.Size optimalSize = null;

        // Start with max value and refine as we iterate over available preview sizes. This is the
        // minimum difference between view and camera height.
        double minDiff = Double.MAX_VALUE;

        // Target view height
        int targetHeight = h;

        // Try to find a preview size that matches aspect ratio and the target view size.
        // Iterate over all available sizes and pick the largest size that can fit in the view and
        // still maintain the aspect ratio.
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find preview size that matches the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }

    public static void setPreviewSizeAndOrientation(Camera camera,
                                                    CamcorderProfile profile,
                                                    int preferredWidth,
                                                    int preferredHeight) {

        Camera.Parameters params = camera.getParameters();

        Camera.Size optimalSize = CameraHelper.getOptimalPreviewSize(params, preferredWidth, preferredHeight);
        int width = optimalSize.width;
        int height = optimalSize.height;

//        int width = preferredWidth;
//        int height = preferredHeight;

        profile.videoFrameWidth = width;
        profile.videoFrameHeight = height;

        params.setPreviewSize(width, height);
        camera.setParameters(params);
    }


    /**
     * @return the front-facing camera, otherwise return the default one.
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static Camera getDefaultCamera(Activity activity) {
        int numberOfCameras = Camera.getNumberOfCameras();

        // try to find the front camera
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera camera = null;
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                camera = Camera.open(i);
                break;
            }
        }

        if (camera == null && numberOfCameras > 0) {
            // get the first camera
            Camera.getCameraInfo(0, cameraInfo);
            camera = Camera.open(0);
        }

        if (camera == null) {
            Timber.e("Device has no camera");
            return null;
        }

        int rotation = activity
                .getWindowManager()
                .getDefaultDisplay()
                .getRotation();

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }

        camera.setDisplayOrientation(result);
        return camera;
    }

}

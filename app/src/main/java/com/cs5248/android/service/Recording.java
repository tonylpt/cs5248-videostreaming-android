package com.cs5248.android.service;

import com.cs5248.android.model.VideoSegment;

import rx.Observable;

/**
 * @author lpthanh
 */
public interface Recording {
    public void startRecording();
    public void stopRecording();
    public void startUploading();
}

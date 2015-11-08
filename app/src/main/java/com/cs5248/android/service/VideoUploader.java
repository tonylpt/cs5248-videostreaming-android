package com.cs5248.android.service;


import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Created by larcuser on 19/10/15.
 */
public enum VideoUploader {

    INSTANCE;
    String videoFolder = Environment.getExternalStorageDirectory() + "/video/";
    UploadStrategy uploadStrategy = UploadStrategy.LEAST_RECENT_FIRST;
    List<String> videoUploadQueue = new ArrayList<String>();
    Map<String,UploadStatus> videoUploadStatus = new HashMap<String,UploadStatus>();

    String currentVideoId = "";

    public enum UploadStatus{
        NOT_UPLOADED,
        UPLOADING,
        UPLOAD_FAILED,
        UPLOADED
    }

    public enum UploadStrategy{
        MOST_RECENT_FIRST,
        LEAST_RECENT_FIRST,
    }

    public void updateVideoListWithNewFile(){
        File[] videoFileArray = new File(videoFolder).listFiles();
        List<String> videoList = new ArrayList<String>();
        for(File file : videoFileArray){
            if(file.getName().contains(currentVideoId)) {
                videoList.add(file.getName());
            }
        }
        Collections.sort(videoList, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                return lhs.compareToIgnoreCase(rhs);
            }
        });
        synchronized(videoUploadStatus){
            Set<String> currentVideoList = videoUploadStatus.keySet();
            for(String file : videoList) {
                if (!currentVideoList.contains(file)){
                    videoUploadStatus.put(file,UploadStatus.NOT_UPLOADED);
                }
            }
        }
        updateVideoUploadQueue();
    }

    public void updateVideoStatusWhenUploadFailed(String file) {
        synchronized (videoUploadStatus) {
            videoUploadStatus.put(file,UploadStatus.UPLOAD_FAILED);
        }
        updateVideoUploadQueue();
    }

    public void updateVideoUploadQueue(){
        synchronized(videoUploadStatus){
            synchronized(videoUploadQueue) {
                List<String> currentVideoInQueue = videoUploadQueue;
                List<String> currentVideoUploadStatus = new ArrayList<String>(videoUploadStatus.keySet());
                List<String> currentVideoNotUploading = new ArrayList<String>();

                for (String file : currentVideoUploadStatus) {
                    if (videoUploadStatus.get(file) != UploadStatus.UPLOADING) {
                        currentVideoNotUploading.add(file);
                    }
                }
                for (int i = 0; i < currentVideoNotUploading.size(); i++) {
                    String currentFile = currentVideoNotUploading.get(i);
                    if (!currentVideoInQueue.contains(currentFile)) {
                        videoUploadQueue.add(currentFile);
                    }
                }

                if (uploadStrategy == UploadStrategy.MOST_RECENT_FIRST) {
                    Collections.sort(videoUploadQueue, new Comparator<String>() {
                        @Override
                        public int compare(String lhs, String rhs) {
                            return lhs.compareToIgnoreCase(rhs);
                        }
                    });
                }
                if (uploadStrategy == UploadStrategy.LEAST_RECENT_FIRST) {
                    Collections.sort(videoUploadQueue, new Comparator<String>() {
                        @Override
                        public int compare(String lhs, String rhs) {
                            return rhs.compareToIgnoreCase(lhs);
                        }
                    });
                }
            }
        }

    }
    public void uploadVideoFromQueue(String videoId) {
        this.currentVideoId = videoId;
        while(true){
            synchronized(videoUploadQueue){
                synchronized(videoUploadStatus){
                    if(videoUploadQueue.size() > 0){
                        String fileToUpload = videoUploadQueue.get(0);
                        videoUploadQueue.remove(0);
                        VideoTaskExecutor.INSTANCE.uploadVideo(fileToUpload);
                    }
                    try {
                        Thread.sleep(500);
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

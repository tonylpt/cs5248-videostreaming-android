package com.cs5248.android.service;

import android.os.AsyncTask;
import android.os.Environment;

import com.cs5248.android.model.VideoSegment;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.inject.Inject;
public enum VideoTaskExecutor {

    INSTANCE;

    @Inject
    StreamingService streamingService;

    RunVideoUploadSchedulerAsyncTask taskUploadScheduler = null;
    String currentVideoId = "0";

    private class WriteVideoFileAsyncTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... files) {
            String inputFile = files[0];
            String outputFile = files[1];

            CameraService.Mp4ContainerWriter writer = new CameraService.Mp4ContainerWriter();
            writer.writeVideoFile(inputFile, outputFile);
            return null;
        }
        protected void onPostExecute(String result) {
            VideoUploader.INSTANCE.updateVideoListWithNewFile();
        }
    }
    private class UploadVideoFileAsyncTask extends AsyncTask<String, Void, Void> {
        String videoFolder = Environment.getExternalStorageDirectory() + "/video/";

        @Override
        protected Void doInBackground(String... files) {
            String filename = files[0];
            Long videoId = Long.parseLong(filename.split("_")[0]);
            Long videoSequenceId = Long.parseLong(filename.split("_")[1]);

            try {
                byte[] videoData = readFile(videoFolder + filename);
                VideoSegment videoSegment = new VideoSegment();
                videoSegment.setVideoId(videoId);
//                videoSegment.setSequenceIndex(videoSequenceId);
//                streamingService.uploadVideoSegment(videoSegment);
            }
            catch(Exception e){
                e.printStackTrace();
            }

            return null;
        }
        protected void onPostExecute(String result) {
            //if result is good then delete file
        }
    }
    private class RunVideoUploadSchedulerAsyncTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... files) {
            String videoId = files[0];
            VideoUploader.INSTANCE.uploadVideoFromQueue(videoId);
            return null;
        }

    }

    public void writeMp4File(String input,String output){
        WriteVideoFileAsyncTask task = new WriteVideoFileAsyncTask();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,input,output);
    }

    public void uploadVideo(String videoId){
        UploadVideoFileAsyncTask task = new UploadVideoFileAsyncTask();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,videoId);
    }

    public void runUploadScheduler(String videoId){
        taskUploadScheduler = new RunVideoUploadSchedulerAsyncTask();
        taskUploadScheduler.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,videoId);
    }

    public void stopUploadScheduler(){
        taskUploadScheduler.cancel(false);
    }

    public  byte[] readFile(String file) throws IOException {
        return readFile(new File(file));
    }

    public  byte[] readFile(File file) throws IOException {
        // Open file
        RandomAccessFile f = new RandomAccessFile(file, "r");
        try {
            // Get and check length
            long longlength = f.length();
            int length = (int) longlength;
            if (length != longlength)
                throw new IOException("File size >= 2 GB");
            // Read file and return data
            byte[] data = new byte[length];
            f.readFully(data);
            return data;
        } finally {
            f.close();
        }
    }

}

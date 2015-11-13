package com.cs5248.android.service;

/**
 * Created by larcuser on 7/11/15.
 */

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.coremedia.iso.boxes.Container;
import com.cs5248.android.model.VideoSegment;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.tracks.h264.H264TrackImpl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CameraService {

//    private CameraPreview cameraPreview;
    private Encoder encoder;
    private Mp4ContainerWriter mp4Writer;
    private static final int frameWidth = 1280;
    private static final int frameHeight = 720;
    private static final int fps = 60;
    private static final int bitrate = 60000000;
    private int secondPerSegment = 3;

    private long videoId = 0;
    private long videoSequenceId = 0;
    private static String pathPrefix = Environment.getExternalStorageDirectory() + "/video/track_";

    List<List<Byte>> videoSegmentsData = null;
    private List<Byte> latestSegmentData = new ArrayList<>();
    boolean firstTime = true;
    private long startWhen = System.nanoTime();
    private long desiredEnd = startWhen + secondPerSegment * 1000000000L;
    private String currentEncoderOutputFileName = null;
    private Object doneOneSegmentMutex = null;
    BufferedOutputStream outputStream = null;

    public CameraService() {

    }

    public CameraService(Context context, Long videoId) {//}, Object doneOneSegmentMutex) {
        //this.doneOneSegmentMutex = doneOneSegmentMutex;
        encoder = new Encoder();
        this.videoId = videoId;
        mp4Writer = new Mp4ContainerWriter();
        videoSegmentsData = new ArrayList<>();
//        cameraPreview = new CameraPreview(context);
    }

    public void startRecording() {
//        cameraPreview.startStreaming();
    }

    public void stopRecording() {
//        cameraPreview.startStreaming();
    }

    public List<Byte> getLatestSegmentData() {
        return videoSegmentsData.get(videoSegmentsData.size() - 1);
    }

    public void receiveCameraFrameCallback(byte[] data) {
        if (outputStream == null) {
            try {
                long currentTime = System.nanoTime();
                desiredEnd = currentTime + secondPerSegment * 1000000000L;
                String filename = String.valueOf(videoSequenceId);
                videoSequenceId = videoSequenceId + 1;
                this.currentEncoderOutputFileName = filename;
                String encoderOutputPath = pathPrefix + filename + ".h264";
                outputStream = new BufferedOutputStream(new FileOutputStream(encoderOutputPath));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            byte[] encoderOutput = encoder.offerEncoder(data);
            if (encoderOutput != null) {
                try {
                    outputStream.write(encoderOutput, 0, encoderOutput.length);
                    if (System.nanoTime() > desiredEnd) {
                        outputStream.close();
                        outputStream = null;
                        mp4Writer.writeVideoFile(pathPrefix + currentEncoderOutputFileName + ".h264",
                                pathPrefix + currentEncoderOutputFileName + ".mp4");
                        Log.d("Write file", "Yay");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

//    private void receiveCameraFrameCallback(byte[] data){
//        if(firstTime){
//            try{
//                long currentTime = System.nanoTime();
//                desiredEnd = currentTime + secondPerSegment * 1000000000L;
//                latestSegmentData = new ArrayList<>();
//                firstTime = false;
//            }
//            catch(Exception e){
//                e.printStackTrace();
//            }
//        }
//        else{
//            Byte[] encoderOutput = encoder.offerEncoder(data);
//            if(encoderOutput != null) {
//                try {
//                    latestSegmentData.addAll(Arrays.asList(encoderOutput));
//
//                    if( System.nanoTime() > desiredEnd){
//                        videoSegmentsData.add(latestSegmentData);
//                        firstTime = true;
//                        doneOneSegmentMutex.notify();
//                    }
//                }
//                catch(Exception e){
//                    e.printStackTrace();
//                }
//            }
//        }
//    }

    public static class Mp4ContainerWriter {
        public void writeVideoFile(String input, String output) {
            try {
                H264TrackImpl h264Track = new H264TrackImpl(new FileDataSourceImpl(input));
                Movie movie = new Movie();
                movie.addTrack(h264Track);
                Container mp4file = new DefaultMp4Builder().build(movie);
                FileChannel fc = new FileOutputStream(new File(output)).getChannel();
                mp4file.writeContainer(fc);
                fc.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class Encoder {
        private MediaCodec mediaCodec;
        private byte[] sps;
        private byte[] pps;

        Encoder() {
            try {
                mediaCodec = MediaCodec.createEncoderByType("video/avc");
                MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", frameWidth, frameHeight);
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
                mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                mediaCodec.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        private void close() throws IOException {
            mediaCodec.stop();
            mediaCodec.release();
        }


        private byte[] offerEncoder(byte[] data) {
            try {
                byte[] input = swapYV12toI420(data, frameWidth, frameHeight);
                ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
                int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    inputBuffer.clear();
                    inputBuffer.put(input);
                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, 0, 0);
                }
                ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                if (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                    byte[] outData = new byte[bufferInfo.size];
                    //Byte[] output = new Byte[bufferInfo.size];
                    outputBuffer.get(outData);
                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                    //for(int i = 0; i < outData.length;i++){
                    //    output[i] = Byte.valueOf(outData[i]);
                    //}
                    return outData;

                }

            } catch (Throwable t) {
                t.printStackTrace();
            }
            return null;

        }

        private byte[] swapYV12toI420(byte[] yv12bytes, int width, int height) {
            byte[] i420bytes = new byte[yv12bytes.length];
            for (int i = 0; i < width * height; i++)
                i420bytes[i] = yv12bytes[i];
            for (int i = width * height; i < width * height + (width / 2 * height / 2); i++)
                i420bytes[i] = yv12bytes[i + (width / 2 * height / 2)];
            for (int i = width * height + (width / 2 * height / 2); i < width * height + 2 * (width / 2 * height / 2); i++)
                i420bytes[i] = yv12bytes[i - (width / 2 * height / 2)];
            return i420bytes;
        }

    }


}

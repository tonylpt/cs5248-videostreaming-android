package com.cs5248.android.ui;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.cs5248.android.R;
import com.cs5248.android.model.Video;
import com.cs5248.android.service.StreamingService;
import com.cs5248.android.service.StreamingSession;
import com.cs5248.android.service.StreamingState;
import com.cs5248.android.util.BaseActivity;
import com.cs5248.android.util.Util;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.OnClick;
import timber.log.Timber;

abstract class StreamingActivity extends BaseActivity {

    @Inject
    StreamingService streamingService;

    @Bind(R.id.play_pause_button)
    Button playPauseButton;


    MediaPlayerService playerService = new MediaPlayerService();

    List<StreamingSession.Streamlet> streamletsQueue = new ArrayList<>();
    int currentSegmentBeingPlayed = 0;
    boolean firstTime = true;
    boolean streamEnded = false;

    private StreamingSession session;
    private final StreamingSession.StreamingListener streamingListener = new StreamingSession.StreamingListener() {
        @Override
        public void streamletDownloaded(StreamingSession.Streamlet streamlet) {
            runOnUiThread(() -> onStreamletDownloaded(streamlet));
        }

        @Override
        public void noMoreStreamlet() {
            runOnUiThread(() -> onStreamingEnded());
        }
    };


    @Override
    protected void initActivity(Bundle savedInstanceState) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Video video = Util.getParcelable(this, "video", Video.class);
        if (video != null) {
            this.session = streamingService.openSession(video, isLiveStreaming());
            this.session.setStreamingListener(streamingListener);

        } else {
            Timber.e("Could not find a video parcelable for this activity");
        }

        if (session == null) {
            playPauseButton.setEnabled(false);
        }
    }

    protected abstract boolean isLiveStreaming();

    private void onStreamletDownloaded(StreamingSession.Streamlet streamlet) {
        // just to demonstrate that the UI can be safely updated from here
        // this method may not actually be needed.

        // we can just set up a polling loop when the streaming starts and
        // poll the session by:  session.getNextStreamlet() when we need the next
        // segment. Once we finish playing the segment, clean it up by:
        // session.clearStreamlet(streamlet)

        playPauseButton.setText("Downloaded: " + streamlet.getTargetFile().getName());
    }

    private void onStreamingEnded() {
        playPauseButton.setText("No more segment");
        streamEnded = true;
    }
    abstract public RelativeLayout getPlayerContainer();

    @OnClick(R.id.play_pause_button)
    public void onPlayPauseClick() {
        if (session == null) {
            return;
        }


        if (session.isProgressing()) {
            session.endStreaming();
        } else {
            session.startStreaming();
        }

        startNextStreamlet();
    }

    public void startNextStreamlet(){
        try {
            //first time need to prepare video player first
            if(firstTime){
                StreamingSession.Streamlet streamlet = getNextStreamlet();
                if(streamlet != null) {
                    playerService.prepareBufferedMediaPlayer(streamlet);
                }
                firstTime = false;
            }
            //not the first time, we should already have the next segment downloaded and prepared in buffer
            //otherwise startMediaPlayer method will wait
            else{
                playerService.startMediaPlayer();
            }

        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public StreamingSession.Streamlet getNextStreamlet(){
        StreamingSession.Streamlet streamlet = null;
        while((streamlet = session.getNextStreamlet()) == null){
            try {
                Thread.sleep(100);
                if(streamEnded){
                    return null;
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        return streamlet;
    }


    public class MediaPlayerService{

        MediaPlayer bufferedMediaPlayer = null;
        private SurfaceHolder  bufferedHolder = null;
        private SurfaceView       bufferedSurface = null;

        MediaPlayer currentMediaPlayer = null;
        private SurfaceHolder  currentHolder = null;
        private SurfaceView       currentSurface = null;

        private void prepareBufferedMediaPlayer(StreamingSession.Streamlet streamlet) {

            this.bufferedSurface = new SurfaceView(StreamingActivity.this);
            this.bufferedHolder = this.bufferedSurface.getHolder();
            getPlayerContainer().addView(bufferedSurface, 0);
            this.bufferedHolder.addCallback(new SurfaceHolder.Callback() {

                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    bufferedMediaPlayer = new MediaPlayer();
                    bufferedMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            mp.release();
                            startNextStreamlet();
                        }
                    });
                    try {
                        bufferedMediaPlayer.setDataSource(streamlet.getTargetFile().getPath());
                        bufferedMediaPlayer.setSurface(bufferedHolder.getSurface());
                        bufferedMediaPlayer.prepare();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    int videoWidth = bufferedMediaPlayer.getVideoWidth();
                    int videoHeight = bufferedMediaPlayer.getVideoHeight();

                    int surfaceWidth = bufferedHolder.getSurfaceFrame().width();
                    ViewGroup.LayoutParams params = bufferedSurface.getLayoutParams();
                    params.width = surfaceWidth;
                    params.height = (int) (((float) videoHeight / (float) videoWidth) * (float) surfaceWidth);
                    bufferedSurface.setLayoutParams(params);

                    bufferedMediaPlayer.start();
                    bufferedMediaPlayer.pause();

                    if (currentMediaPlayer == null) {
                        startMediaPlayer();
                    }
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }
            });
        }

        public void startMediaPlayer(){
            //when the buffered / background player is not prepared we wait, this wait is caused by getNextStreamlet
            while(bufferedMediaPlayer == null){
                try {
                    Thread.sleep(100);
                    if(streamEnded){
                        return;
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }

            SurfaceView previousSurface = this.currentSurface;

            //flip the player
            this.currentMediaPlayer = this.bufferedMediaPlayer;
            this.currentHolder = this.bufferedHolder;
            this.currentSurface = this.bufferedSurface;

            if (this.currentMediaPlayer != null) {
                this.currentMediaPlayer.start();
            }

            getPlayerContainer().removeView(previousSurface);

            this.bufferedMediaPlayer = null;
            this.bufferedHolder = null;
            this.bufferedSurface = null;

            StreamingSession.Streamlet streamlet = getNextStreamlet();
            if(streamlet != null)
                prepareBufferedMediaPlayer(streamlet);
        }
    };


}


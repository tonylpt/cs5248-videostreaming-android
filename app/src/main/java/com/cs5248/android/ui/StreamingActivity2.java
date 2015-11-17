package com.cs5248.android.ui;

import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.cs5248.android.R;
import com.cs5248.android.model.Video;
import com.cs5248.android.service.StreamingService;
import com.cs5248.android.service.StreamingSession;
import com.cs5248.android.util.BaseActivity;
import com.cs5248.android.util.Util;

import java.io.File;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.OnClick;
import lombok.Getter;
import timber.log.Timber;

import static com.cs5248.android.service.StreamingSession.Streamlet;

abstract class StreamingActivity2 extends BaseActivity {

    @Inject
    StreamingService streamingService;

    @Bind(R.id.play_pause_button)
    Button playPauseButton;

    @Bind(R.id.wait_view)
    View waitView;

    @Bind(R.id.player_surface_1)
    SurfaceView playerSurface1;

    @Bind(R.id.player_surface_2)
    SurfaceView playerSurface2;


    private ExecutorService loadExec;

    private SurfaceView[] surfaceViews;

    private LinkedList<StreamletPlayer> bufferingPlayerQueue = new LinkedList<>();

    private LinkedList<StreamletPlayer> idlePlayerQueue = new LinkedList<>();

    private StreamingSession session;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.loadExec = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onDestroy() {
        this.loadExec.shutdown();
        super.onDestroy();
    }

    @Override
    protected void initActivity(Bundle savedInstanceState) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        this.surfaceViews = new SurfaceView[]{playerSurface1, playerSurface2};
        for (SurfaceView surface : surfaceViews) {
            idlePlayerQueue.push(new StreamletPlayer(surface));
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
    }

    private void hideWaitView() {
        waitView.setVisibility(View.GONE);
    }

    private void playNextSegment() {
        StreamletPlayer bufferingPlayer = bufferingPlayerQueue.poll();
        if (bufferingPlayer != null) {
            hideWaitView();
            bufferingPlayer.play();
        }

        StreamletPlayer idlePlayer = idlePlayerQueue.poll();
        if (idlePlayer != null) {
            Streamlet streamlet = session.getNextStreamlet();
            if (streamlet != null) {
                idlePlayer.reset();
                (new AsyncTask<Streamlet, Void, Void>() {
                    @Override
                    protected Void doInBackground(Streamlet[] streamlet) {
                        idlePlayer.setStreamlet(streamlet[0]);
                        return null;
                    }
                }).executeOnExecutor(loadExec, streamlet);
                bufferingPlayerQueue.push(idlePlayer);
            }
        }
    }

    private void onPlayerCompleted(StreamletPlayer player, Streamlet streamlet) {
        session.clearStreamlet(streamlet);
        idlePlayerQueue.push(player);
        player.shelve();
        playNextSegment();
    }

    private void onPlayerError(StreamletPlayer player, Streamlet streamlet) {
        session.clearStreamlet(streamlet);
        idlePlayerQueue.push(player);
        player.shelve();
        playNextSegment();
    }


    private void onStreamletDownloaded(StreamingSession.Streamlet streamlet) {
        // just to demonstrate that the UI can be safely updated from here
        // this method may not actually be needed.

        // we can just set up a polling loop when the streaming starts and
        // poll the session by:  session.getNextStreamlet() when we need the next
        // segment. Once we finish playing the segment, clean it up by:
        // session.clearStreamlet(streamlet)

        playNextSegment();
    }

    private void onStreamingEnded() {
        playPauseButton.setText("Streaming Ended");

        waitView.setVisibility(View.INVISIBLE);
        for (SurfaceView surface : surfaceViews) {
            surface.setVisibility(View.GONE);
        }
    }

    private class StreamletPlayer implements SurfaceHolder.Callback,
            MediaPlayer.OnCompletionListener,
            MediaPlayer.OnErrorListener,
            MediaPlayer.OnBufferingUpdateListener {


        public static final int MIN_BUFFER_PERCENT = 20;

        private final MediaPlayer mediaPlayer = new MediaPlayer();

        private final CountDownLatch initLatch = new CountDownLatch(1);

        private final SurfaceView surfaceView;

        private final SurfaceHolder surfaceHolder;

        @Getter
        private Streamlet currentStreamlet;

        private CountDownLatch bufferLatch;

        private CountDownLatch playLatch;

        @Getter
        private boolean error;

        public StreamletPlayer(SurfaceView surfaceView) {
            this.surfaceView = surfaceView;
            this.surfaceHolder = surfaceView.getHolder();
            this.surfaceHolder.addCallback(this);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            initLatch.countDown();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            // ignored
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // ignored
        }

        public void waitForReady() {
            try {
                initLatch.await();
            } catch (InterruptedException e) {
                Timber.e(e, "Interrupted!");
            }
        }

        public synchronized void waitForBuffer() {
            if (bufferLatch == null) {
                return;
            }

            try {
                bufferLatch.await();
            } catch (InterruptedException e) {
                Timber.e(e, "Interrupted!");
            }
        }

        public synchronized void reset() {
            this.currentStreamlet = null;
            this.bufferLatch = new CountDownLatch(1);
            this.playLatch = new CountDownLatch(1);
            this.error = false;
        }

        public synchronized void setStreamlet(Streamlet streamlet) {
            waitForReady();

            this.currentStreamlet = streamlet;
            File file = streamlet.getTargetFile();

            try {
                MediaPlayer mediaPlayer = this.mediaPlayer;

                mediaPlayer.setDataSource(file.getAbsolutePath());
                mediaPlayer.setSurface(surfaceHolder.getSurface());

                mediaPlayer.setOnBufferingUpdateListener(this);
                mediaPlayer.setOnErrorListener(this);
                mediaPlayer.setOnCompletionListener(this);

                mediaPlayer.prepare();

                playLatch.countDown();

                // setting size, keeping ratio
                int videoWidth = mediaPlayer.getVideoWidth();
                int videoHeight = mediaPlayer.getVideoHeight();

                int surfaceWidth = surfaceHolder.getSurfaceFrame().width();
                ViewGroup.LayoutParams params = surfaceView.getLayoutParams();
                params.width = surfaceWidth;
                params.height = (int) (((float) videoHeight / (float) videoWidth) * (float) surfaceWidth);
                surfaceView.setLayoutParams(params);

            } catch (Exception e) {
                Timber.e(e, "Error setting player source to %s", file.getAbsolutePath());
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            mediaPlayer.reset();
            onPlayerCompleted(this, currentStreamlet);
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Timber.e("An error occurred while playing streamlet. What=%d", what);

            bufferLatch.countDown();
            error = true;
            mediaPlayer.reset();

            onPlayerError(this, currentStreamlet);
            return true;
        }

        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            if (percent >= MIN_BUFFER_PERCENT) {
                bufferLatch.countDown();
            }
        }

        public void play() {
            if (!error) {
                try {
                    playLatch.await();
                } catch (InterruptedException e) {
                    Timber.e(e, "Interrupted!");
                }

                surfaceView.setVisibility(View.VISIBLE);
                mediaPlayer.start();
            }
        }

        public void shelve() {
            surfaceView.setVisibility(View.GONE);
        }
    }

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

}


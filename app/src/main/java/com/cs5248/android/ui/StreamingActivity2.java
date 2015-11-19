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
import android.widget.LinearLayout;

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
import java.util.concurrent.Semaphore;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.OnClick;
import lombok.Getter;
import timber.log.Timber;

import static com.cs5248.android.service.StreamingSession.Streamlet;

/**
 * This is an alternative implementation of the StreamingActivity that continuously switches between
 * two or more surface views instead of continuously creating new ones and destroying old ones.
 * This was created after the course project has been submitted and finalized.
 *
 * @author lpthanh
 */
abstract class StreamingActivity2 extends BaseActivity {

    @Inject
    StreamingService streamingService;

    @Bind(R.id.play_pause_button)
    Button playPauseButton;

    @Bind(R.id.player_surface_container)
    LinearLayout playerSurfaceContainer;

    @Bind(R.id.wait_view)
    View waitView;

    @Bind(R.id.player_surface_1)
    SurfaceView playerSurface1;

    @Bind(R.id.player_surface_2)
    SurfaceView playerSurface2;

    @Bind(R.id.player_surface_3)
    SurfaceView playerSurface3;


    private ExecutorService loadExec;

    private StreamletPlayer[] players;

    private final LinkedList<StreamletPlayer> bufferingPlayerQueue = new LinkedList<>();

    private LinkedList<StreamletPlayer> idlePlayerQueue = new LinkedList<>();

    private StreamingSession session;

    private volatile boolean playerStarted;

    private volatile String currentPlayingMedia;

    private volatile String currentBufferingMedia;

    private volatile boolean noMoreSegment;

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

        SurfaceView[] surfaceViews = new SurfaceView[]{playerSurface1, playerSurface2, playerSurface3};
        StreamletPlayer[] players = this.players = new StreamletPlayer[surfaceViews.length];
        for (int i = 0, l = players.length; i < l; ++i) {
            StreamletPlayer player = new StreamletPlayer(i, surfaceViews[i]);
            players[i] = player;
            idlePlayerQueue.add(player);

            Timber.d("A player is ready.");
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
            playPauseButton.setText("CONNECTING...");
            playPauseButton.setEnabled(false);
            session.startStreaming();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (session.isProgressing()) {
            session.endStreaming();
        }

        for (StreamletPlayer player : players) {
            player.dispose();
        }
    }

    private void setWaiting(boolean isWaiting) {
        waitView.setVisibility(isWaiting ? View.VISIBLE : View.GONE);
    }

    private void showAndPlay(StreamletPlayer player) {
        player.play();

        for (StreamletPlayer p : players) {
            if (p != player) {
                p.hide();
            }
        }
    }

    private void bufferNextSegment(boolean async) {
        StreamletPlayer idlePlayer = idlePlayerQueue.poll();
        if (idlePlayer != null) {
            Streamlet streamlet = session.getNextStreamlet();
            if (streamlet != null) {
                idlePlayer.reset();

                /**
                 * An inner class that can buffer a streamlet either as an AsyncTask or directly
                 * on the current thread.
                 */
                class BufferTask extends AsyncTask<Streamlet, Void, Void> {

                    public void bufferStreamlet(Streamlet streamlet) {
                        idlePlayer.setStreamlet(streamlet);
                        synchronized (bufferingPlayerQueue) {
                            bufferingPlayerQueue.add(idlePlayer);
                            Timber.d("playNextSegment: pushed an idle player into buffering queue, file=%s",
                                    streamlet.getTargetFile().getAbsolutePath());

                            currentBufferingMedia = idlePlayer.getCurrentMedia();
                            updateStatus();
                        }
                    }

                    @Override
                    protected Void doInBackground(Streamlet[] streamlet) {
                        bufferStreamlet(streamlet[0]);
                        return null;
                    }
                }

                BufferTask bufferTask = new BufferTask();
                if (async) {
                    bufferTask.executeOnExecutor(loadExec, streamlet);
                } else {
                    bufferTask.bufferStreamlet(streamlet);
                }
            } else {
                Timber.d("No new downloaded streamlet for player %d", idlePlayer.playerId);
            }
        } else {
            Timber.d("No available player for buffering");
        }
    }

    private void playNextBuffer() {
        synchronized (bufferingPlayerQueue) {
            StreamletPlayer bufferingPlayer = bufferingPlayerQueue.poll();
            if (bufferingPlayer != null) {
                playerStarted = true;
                setWaiting(false);
                showAndPlay(bufferingPlayer);
                Timber.d("playNextSegment: dequeued a player and played it");

                currentPlayingMedia = bufferingPlayer.getCurrentMedia();
                updateStatus();
            } else {
                Timber.d("playNextSegment: no player in buffering queue");
            }
        }
    }

    private void onPlayerCompleted(StreamletPlayer player, Streamlet streamlet) {
        idlePlayerQueue.add(player);

        Timber.d("Player completed file %s and has been shelved",
                streamlet.getTargetFile().getAbsolutePath());

        playNextBuffer();
        bufferNextSegment(true);

        session.clearStreamlet(streamlet);
    }

    private void onPlayerError(StreamletPlayer player, Streamlet streamlet) {
        session.clearStreamlet(streamlet);
        idlePlayerQueue.add(player);
        playNextBuffer();
        bufferNextSegment(true);
    }


    private void onStreamletDownloaded(StreamingSession.Streamlet streamlet) {
        // just to demonstrate that the UI can be safely updated from here
        // this method may not actually be needed.

        // we can just set up a polling loop when the streaming starts and
        // poll the session by:  session.getNextStreamlet() when we need the next
        // segment. Once we finish playing the segment, clean it up by:
        // session.clearStreamlet(streamlet)

        if (playerStarted) {
            bufferNextSegment(true);
        } else {
            // for the first segment, wait for the first buffer and play it
            bufferNextSegment(false);
            playNextBuffer();
        }
    }

    private void onPlayerBuffered(StreamletPlayer player, Streamlet streamlet) {
        if (!playerStarted) {
            // this is the first segment
            // this must happen after the first onStreamletDownloaded() call and
            // there was no player already in buffer queue. So we call playNextSegment()
            // again to start playing

//            Timber.d("The first player has been bufferred enough. Now starting to play.");
//            playNextBuffer();
        }
    }

    private void onStreamingEnded() {
        noMoreSegment = true;
        setWaiting(false);
        for (StreamletPlayer player : players) {
            player.hide();
        }
    }

    private void updateStatus() {
        runOnUiThread(() -> {
            String status = String.format("Buffering %s | Playing %s | %s",
                    currentBufferingMedia, currentPlayingMedia, noMoreSegment ? "END" : "");
            playPauseButton.setText(status);
        });
    }

    private class StreamletPlayer implements SurfaceHolder.Callback,
            MediaPlayer.OnCompletionListener,
            MediaPlayer.OnErrorListener,
            MediaPlayer.OnBufferingUpdateListener {


        public static final int MIN_BUFFER_PERCENT = 20;

        private final int playerId;

        private final MediaPlayer mediaPlayer = new MediaPlayer();

        private final CountDownLatch initLatch = new CountDownLatch(1);

        private final SurfaceView surfaceView;

        private final SurfaceHolder surfaceHolder;

        @Getter
        private Streamlet currentStreamlet;

        private Semaphore playerResetingLock;

        @Getter
        private boolean error;

        private int videoWidth;

        private int videoHeight;

        private boolean surfaceDestroyed;

        public StreamletPlayer(int playerId, SurfaceView surfaceView) {
            this.playerId = playerId;
            this.surfaceView = surfaceView;
            this.surfaceHolder = surfaceView.getHolder();
            this.surfaceHolder.addCallback(this);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Timber.d("Surface is created");
            initLatch.countDown();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Timber.d("Surface is changed");
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            surfaceDestroyed = true;
            Timber.d("Surface is destroyed");
        }

        public void waitForReady() {
            try {
                initLatch.await();
            } catch (InterruptedException e) {
                Timber.e(e, "Interrupted!");
            }
        }

        public synchronized void reset() {
            this.currentStreamlet = null;
            this.error = false;
            if (playerResetingLock != null) {
                playerResetingLock.release();
            }

            this.playerResetingLock = new Semaphore(0);
        }

        public synchronized void setStreamlet(Streamlet streamlet) {
            waitForReady();
            if (playerResetingLock == null) {
                throw new RuntimeException("Player Reseting Lock is null!");
            }

            this.currentStreamlet = streamlet;
            File file = streamlet.getTargetFile();

            try {
                if (surfaceDestroyed) {
                    return;
                }

                MediaPlayer mediaPlayer = this.mediaPlayer;
                mediaPlayer.reset();
                mediaPlayer.setDataSource(file.getAbsolutePath());
                try {
                    mediaPlayer.setSurface(surfaceHolder.getSurface());
                } catch (IllegalArgumentException e) {
                    // most probably surface has been destroyed
                    return;
                }

                mediaPlayer.setOnBufferingUpdateListener(this);
                mediaPlayer.setOnErrorListener(this);
                mediaPlayer.setOnCompletionListener(this);

                mediaPlayer.prepare();
                mediaPlayer.start();
                mediaPlayer.pause();

                this.videoWidth = mediaPlayer.getVideoWidth();
                this.videoHeight = mediaPlayer.getVideoHeight();

            } catch (Exception e) {
                Timber.e(e, "Error setting player source to %s", file.getAbsolutePath());
                throw new RuntimeException(e);
            } finally {
                playerResetingLock.release();

                Timber.d("Player %d is setup for streamlet %s",
                        playerId, streamlet.getTargetFile().getAbsolutePath());
            }
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            onPlayerCompleted(this, currentStreamlet);
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Timber.e("Player %d: An error occurred while playing streamlet. What=%d",
                    playerId, what);

            error = true;
            onPlayerError(this, currentStreamlet);
            return true;
        }

        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            if (percent >= MIN_BUFFER_PERCENT) {
                onPlayerBuffered(this, currentStreamlet);
            }
        }

        public synchronized void play() {
            if (!error) {
                try {
                    playerResetingLock.acquire();

                    Timber.d("Player %d is playing streamlet: %s", playerId,
                            currentStreamlet.getTargetFile().getAbsolutePath());

                    show();
                    mediaPlayer.start();

                } catch (InterruptedException e) {
                    Timber.e(e, "Interrupted!");
                }
            }
        }

        public void show() {
            int surfaceWidth = surfaceHolder.getSurfaceFrame().width();
            ViewGroup.LayoutParams params = surfaceView.getLayoutParams();
            params.width = surfaceWidth;
            params.height = (int) (((float) videoHeight / (float) videoWidth) * (float) surfaceWidth);
            surfaceView.setLayoutParams(params);
        }

        public void hide() {
            int surfaceWidth = surfaceHolder.getSurfaceFrame().width();
            ViewGroup.LayoutParams params = surfaceView.getLayoutParams();
            params.width = surfaceWidth;
            params.height = 0;
            surfaceView.setLayoutParams(params);
        }

        public String getCurrentMedia() {
            if (currentStreamlet == null) {
                return null;
            }

            return currentStreamlet.getMediaName();
        }

        public void dispose() {
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.release();
                } catch (Exception e) {
                    Timber.e(e, "Error releasing player %d", playerId);
                }
            }
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


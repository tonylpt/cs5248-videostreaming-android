package com.cs5248.android.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.widget.Button;

import com.cs5248.android.R;
import com.cs5248.android.model.Video;
import com.cs5248.android.service.StreamingService;
import com.cs5248.android.service.StreamingSession;
import com.cs5248.android.util.BaseActivity;
import com.cs5248.android.util.Util;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.OnClick;
import timber.log.Timber;

import static com.cs5248.android.service.StreamingSession.StreamingListener;
import static com.cs5248.android.service.StreamingSession.Streamlet;


abstract class StreamingActivity extends BaseActivity {

    @Inject
    StreamingService streamingService;

    @Bind(R.id.play_pause_button)
    Button playPauseButton;

    private StreamingSession session;

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


    private void onStreamletDownloaded(Streamlet streamlet) {
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

    private final StreamingListener streamingListener = new StreamingListener() {
        @Override
        public void streamletDownloaded(Streamlet streamlet) {
            runOnUiThread(() -> onStreamletDownloaded(streamlet));
        }

        @Override
        public void noMoreStreamlet() {
            runOnUiThread(() -> onStreamingEnded());
        }
    };

}

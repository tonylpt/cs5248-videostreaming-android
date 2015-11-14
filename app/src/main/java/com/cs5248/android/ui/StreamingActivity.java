package com.cs5248.android.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.widget.Button;

import com.cs5248.android.R;
import com.cs5248.android.model.Video;
import com.cs5248.android.service.StreamingService;
import com.cs5248.android.service.StreamingSession;
import com.cs5248.android.service.StreamingState;
import com.cs5248.android.util.BaseActivity;
import com.cs5248.android.util.Util;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.OnClick;
import timber.log.Timber;

import static com.cs5248.android.service.StreamingSession.StateChangeListener;


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
            this.session.setStateChangeListener(streamingListener);
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

    private final StateChangeListener streamingListener = new StateChangeListener() {

        @Override
        public void stateChanged(StreamingState newState) {

        }

        @Override
        public void streamletDownloaded(StreamingSession.Streamlet streamlet) {

        }
    };

}

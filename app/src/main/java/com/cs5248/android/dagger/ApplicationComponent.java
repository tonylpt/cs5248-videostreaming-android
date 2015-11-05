package com.cs5248.android.dagger;

import com.cs5248.android.StreamingApplication;
import com.cs5248.android.ui.AboutActivity;
import com.cs5248.android.ui.HomeActivity;
import com.cs5248.android.ui.LiveStreamingActivity;
import com.cs5248.android.ui.RecordActivity;
import com.cs5248.android.ui.RecordStep1;
import com.cs5248.android.ui.RecordStep2;
import com.cs5248.android.ui.VideoListFragment;
import com.cs5248.android.ui.VodPlaybackActivity;

import javax.inject.Singleton;

import dagger.Component;

/**
 * @author lpthanh
 */
@Singleton
@Component(modules = ApplicationModule.class)
public interface ApplicationComponent {

    /* The injection hierarchy is flattened here since it's quite simple. */

    void inject(StreamingApplication application);

    void inject(HomeActivity activity);

    void inject(AboutActivity activity);

    void inject(RecordActivity activity);

    void inject(VodPlaybackActivity activity);

    void inject(LiveStreamingActivity activity);

    void inject(VideoListFragment fragment);

    void inject(RecordStep1 fragment);

    void inject(RecordStep2 fragment);

}
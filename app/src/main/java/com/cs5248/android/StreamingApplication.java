package com.cs5248.android;

import android.app.Application;

import com.activeandroid.ActiveAndroid;
import com.cs5248.android.dagger.ApplicationComponent;
import com.cs5248.android.dagger.ApplicationModule;
import com.cs5248.android.dagger.DaggerApplicationComponent;
import com.cs5248.android.service.RecordingService;
import com.cs5248.android.service.StreamingService;
import com.path.android.jobqueue.JobManager;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * @author lpthanh
 */
public class StreamingApplication extends Application {

    private ApplicationComponent applicationComponent;

    @Inject
    private StreamingService streamingService;

    @Inject
    private JobManager jobManager;

    @Inject
    private RecordingService recordingService;


    @Override
    public void onCreate() {
        super.onCreate();

        // for local caching of data
        ActiveAndroid.initialize(this);

        // for logging
        Timber.plant(new Timber.DebugTree());

        applicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .build();
    }

    public ApplicationComponent component() {
        return applicationComponent;
    }

    public StreamingService streamingService() {
        return streamingService;
    }

    public JobManager jobManager() {
        return jobManager;
    }

    public RecordingService recordingService() {
        return recordingService;
    }

}

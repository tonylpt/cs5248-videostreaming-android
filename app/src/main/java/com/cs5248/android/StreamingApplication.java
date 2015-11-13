package com.cs5248.android;

import android.app.Application;

import com.activeandroid.ActiveAndroid;
import com.cs5248.android.dagger.ApplicationComponent;
import com.cs5248.android.dagger.ApplicationModule;
import com.cs5248.android.dagger.DaggerApplicationComponent;
import com.cs5248.android.service.ApiService;
import com.cs5248.android.service.JobService;
import com.cs5248.android.service.RecordingService;
import com.cs5248.android.service.StreamingService;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * @author lpthanh
 */
public class StreamingApplication extends Application {

    private ApplicationComponent applicationComponent;

    @Inject
    ApiService apiService;

    @Inject
    JobService jobService;

    @Inject
    RecordingService recordingService;

    @Inject
    StreamingService streamingService;


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

        applicationComponent.inject(this);
    }

    public ApplicationComponent component() {
        return applicationComponent;
    }

    public ApiService apiService() {
        return apiService;
    }

    public JobService jobService() {
        return jobService;
    }

    public RecordingService recordingService() {
        return recordingService;
    }

    public StreamingService streamingService() {
        return streamingService;
    }

}

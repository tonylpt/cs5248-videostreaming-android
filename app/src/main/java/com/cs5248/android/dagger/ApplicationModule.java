package com.cs5248.android.dagger;

import android.app.Application;
import android.content.Context;

import com.cs5248.android.BuildConfig;
import com.cs5248.android.service.CameraService;
import com.cs5248.android.service.RecordingService;
import com.cs5248.android.service.StreamingService;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.config.Configuration;
import com.path.android.jobqueue.log.CustomLogger;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import timber.log.Timber;

/**
 * @author lpthanh
 */
@Module
public class ApplicationModule {

    private final Application application;

    public ApplicationModule(Application application) {
        this.application = application;
    }

    @Provides
    @Singleton
    public Context provideApplicationContext() {
        return application;
    }

    @Provides
    @Singleton
    public StreamingService provideStreamingService(Context context) {
        return new StreamingService(context);
    }

    @Provides
    @Singleton
    public CameraService provideCameraService() {
        return new CameraService();
    }

    @Provides
    @Singleton
    public JobManager provideJobManager(Context context) {
        Configuration configuration = new Configuration.Builder(context)
                .customLogger(new CustomLogger() {  // logging using Timber
                    @Override
                    public boolean isDebugEnabled() {
                        return BuildConfig.DEBUG;
                    }

                    @Override
                    public void d(String text, Object... args) {
                        Timber.d(text, args);
                    }

                    @Override
                    public void e(Throwable t, String text, Object... args) {
                        Timber.e(t, text, args);
                    }

                    @Override
                    public void e(String text, Object... args) {
                        Timber.e(text, args);
                    }
                })
                .minConsumerCount(1)    // keep at least one consumer alive
                .maxConsumerCount(3)    // up to 2 consumers at a time
                .loadFactor(3)          // 3 jobs per consumer
                .consumerKeepAlive(120) // wait 2 minute
                .build();

        return new JobManager(context, configuration);
    }

    @Provides
    @Singleton
    public RecordingService provideRecordingService(Context context) {
        return new RecordingService(context);
    }

}

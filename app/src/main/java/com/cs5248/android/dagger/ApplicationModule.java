package com.cs5248.android.dagger;

import android.app.Application;
import android.content.Context;

import com.cs5248.android.service.ApiService;
import com.cs5248.android.service.CameraService;
import com.cs5248.android.service.JobService;
import com.cs5248.android.service.RecordingService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

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
    public ApiService provideApiService(Context context) {
        return new ApiService(context);
    }

    @Provides
    @Singleton
    public CameraService provideCameraService() {
        return new CameraService();
    }

    @Provides
    @Singleton
    public JobService provideUrgentJobManager(Context context) {
        return new JobService(context);
    }

    @Provides
    @Singleton
    public RecordingService provideRecordingService(Context context,
                                                    ApiService service,
                                                    JobService jobService) {

        return new RecordingService(context, service, jobService);
    }

}

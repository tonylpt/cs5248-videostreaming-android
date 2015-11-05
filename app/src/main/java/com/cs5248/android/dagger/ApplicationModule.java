package com.cs5248.android.dagger;

import android.app.Application;
import android.content.Context;

import com.cs5248.android.service.StreamingService;

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
    public StreamingService provideStreamingService(Context context) {
        return new StreamingService(context);
    }

}

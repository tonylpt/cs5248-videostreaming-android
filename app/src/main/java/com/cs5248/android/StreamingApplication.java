package com.cs5248.android;

import android.app.Application;

import com.activeandroid.ActiveAndroid;
import com.cs5248.android.dagger.ApplicationComponent;
import com.cs5248.android.dagger.ApplicationModule;
import com.cs5248.android.dagger.DaggerApplicationComponent;

/**
 * @author lpthanh
 */
public class StreamingApplication extends Application {

    private ApplicationComponent applicationComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        // for local caching of data
        ActiveAndroid.initialize(this);

        applicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .build();
    }

    public ApplicationComponent component() {
        return applicationComponent;
    }

}

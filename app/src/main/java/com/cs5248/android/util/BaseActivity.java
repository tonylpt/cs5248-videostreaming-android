package com.cs5248.android.util;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.cs5248.android.StreamingApplication;
import com.cs5248.android.dagger.ApplicationComponent;

import butterknife.ButterKnife;

/**
 * This contains much of the boilerplate code required for Dagger 2 and Butterknife.
 *
 * @author lpthanh
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        injectActivity(component());

        setContentView(getLayoutId());
        ButterKnife.bind(this);

        initActivity(savedInstanceState);
    }

    /**
     * Convenient method to obtain the dagger component for subclasses
     */
    protected final ApplicationComponent component() {
        return ((StreamingApplication) getApplication()).component();
    }

    /**
     * Subclasses just call component.inject(this)
     */
    protected abstract void injectActivity(ApplicationComponent component);

    protected abstract int getLayoutId();

    /**
     * Optionally override this to provide customization on the created views
     */
    protected void initActivity(Bundle savedInstanceState) {
    }

}

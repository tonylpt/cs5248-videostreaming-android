package com.cs5248.android.util;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        injectActivity(component());

        setContentView(getLayoutId());
        ButterKnife.bind(this);

        initActivity(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // this helps to prevent the parent activity from being restarted when
        // pressing the BACK button on the ActionBar
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

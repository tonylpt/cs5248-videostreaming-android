package com.cs5248.android.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.webkit.WebView;

import com.cs5248.android.R;
import com.cs5248.android.dagger.ApplicationComponent;
import com.cs5248.android.util.BaseActivity;

import butterknife.Bind;

/**
 * Activity for showing the project information.
 */
public class AboutActivity extends BaseActivity {

    @Bind(R.id.about_text)
    WebView webView;

    @Override
    protected void initActivity(Bundle savedInstanceState) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        webView.loadDataWithBaseURL(null,
                getString(R.string.about_description),
                "text/html", "UTF-8", null);
        webView.setBackgroundColor(0x00ffffff);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void injectActivity(ApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_about;
    }
}

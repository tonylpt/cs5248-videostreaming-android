package com.cs5248.android.util;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cs5248.android.StreamingApplication;
import com.cs5248.android.dagger.ApplicationComponent;

import butterknife.ButterKnife;

/**
 * This contains much of the boilerplate code required for Dagger 2 and Butterknife.
 *
 * @author lpthanh
 */
public abstract class BaseFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        injectFragment(component());
    }

    /**
     * Convenient method to obtain the dagger component for subclasses
     */
    protected final ApplicationComponent component() {
        return ((StreamingApplication) getActivity().getApplication()).component();
    }

    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(getLayoutId(), container, false);
        ButterKnife.bind(this, view);
        initView(view, savedInstanceState);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    protected void runOnUiThread(Runnable runnable) {
        getActivity().runOnUiThread(runnable);
    }

    /**
     * This must be called from the UI Thread
     */
    protected void runDelayed(Runnable runnable, long delayMillis) {
        new Handler().postDelayed(runnable, delayMillis);
    }

    /**
     * Subclasses just call component.inject(this)
     */
    protected abstract void injectFragment(ApplicationComponent component);

    protected abstract int getLayoutId();

    /**
     * Optionally override this to provide customization on the created views
     */
    protected void initView(View view, Bundle savedInstanceState) {
    }
}

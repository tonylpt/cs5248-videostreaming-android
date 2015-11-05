package com.cs5248.android.ui;

import android.os.Bundle;
import android.view.View;

import com.cs5248.android.R;
import com.cs5248.android.dagger.ApplicationComponent;
import com.cs5248.android.service.Recording;
import com.cs5248.android.util.WizardStep;

import butterknife.OnClick;

/**
 * @author lpthanh
 */
public class RecordStep2 extends WizardStep<Recording> {

    @Override
    public void initView(View view, Bundle savedInstanceState) {
    }

    @OnClick(R.id.next_button)
    public void next() {
//        finishStep();
    }

    @Override
    protected void startStep(Recording lastResult) {

    }

    @Override
    protected void injectFragment(ApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_record_step_2;
    }
}

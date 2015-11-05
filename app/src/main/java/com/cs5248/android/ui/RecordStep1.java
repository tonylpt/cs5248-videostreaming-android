package com.cs5248.android.ui;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;

import com.cs5248.android.R;
import com.cs5248.android.dagger.ApplicationComponent;
import com.cs5248.android.service.Recording;
import com.cs5248.android.service.StreamingService;
import com.cs5248.android.util.Util;
import com.cs5248.android.util.WizardStep;
import com.dd.CircularProgressButton;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.OnClick;
import lombok.Getter;
import lombok.Setter;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @author lpthanh
 */
public class RecordStep1 extends WizardStep<Recording> {

    @Inject
    StreamingService streamingService;

    @Bind(R.id.title_text)
    EditText titleText;

    @Bind(R.id.next_button)
    CircularProgressButton progressButton;

    @Setter
    @Getter
    private boolean videoCreated;

    @Override
    public void initView(View view, Bundle savedInstanceState) {
        progressButton.setIndeterminateProgressMode(true);
    }

    @OnClick(R.id.next_button)
    public void next() {
        // validate
        String title = titleText.getText().toString();
        title = title.trim();
        titleText.setText(title);

        if (title.isEmpty()) {
            Util.showErrorMessage(getContext(), getString(R.string.text_video_error_empty_title));
            return;
        }

        // update UI
        titleText.setEnabled(false);
        progressButton.setProgress(50);

        streamingService.createNewRecording(title)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onCreateSuccess, this::onCreateFailure);
    }

    private void onCreateSuccess(Recording recording) {
        setVideoCreated(true);
        progressButton.setProgress(100);

        // show the success status for a while
        new Handler().postDelayed(() -> finishStep(recording), 1000);
    }

    private void onCreateFailure(Throwable throwable) {
        titleText.setEnabled(true);
        progressButton.setProgress(-1);
        Util.showErrorMessage(getContext(), getString(R.string.text_video_error_server), throwable);
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
        return R.layout.fragment_record_step_1;
    }

}

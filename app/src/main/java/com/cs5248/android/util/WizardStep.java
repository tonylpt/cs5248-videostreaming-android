package com.cs5248.android.util;

import java.lang.ref.WeakReference;

/**
 * @author lpthanh
 */
public abstract class WizardStep<ResultType> extends BaseFragment {

    /* Avoid memory leaks by using WeakReferences */

    private WeakReference<WizardView<ResultType>> parent;

    private WeakReference<WizardStep<ResultType>> previousStep;

    private WeakReference<WizardStep<ResultType>> nextStep;

    private int childIndex = -1;

    protected String getTitle() {
        return this.getClass().getName();
    }

    void setChildOf(WizardView<ResultType> parent, int childIndex) {
        if (parent == null) {
            this.parent = null;
            this.childIndex = -1;
            return;
        }

        this.parent = new WeakReference<>(parent);
        this.childIndex = childIndex;
    }

    void setPreviousStep(WizardStep<ResultType> step) {
        this.previousStep = new WeakReference<>(step);
    }

    void setNextStep(WizardStep<ResultType> step) {
        this.nextStep = new WeakReference<>(step);
    }

    WizardView<ResultType> getParent() {
        return parent == null ? null : parent.get();
    }

    WizardStep<ResultType> getPreviousStep() {
        return previousStep == null ? null : previousStep.get();
    }

    WizardStep<ResultType> getNextStep() {
        return nextStep == null ? null : nextStep.get();
    }

    int getChildIndex() {
        return childIndex;
    }

    /**
     * @param lastResult For the first step, this argument is null.
     */
    protected abstract void startStep(ResultType lastResult);

    protected final void finishStep(ResultType result) {
        WizardView parent = getParent();
        if (parent != null) {
            parent.onStepFinished(this, result);
        }
    }
}

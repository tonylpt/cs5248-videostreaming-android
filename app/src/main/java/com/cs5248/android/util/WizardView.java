package com.cs5248.android.util;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple implementation of ViewPager to provide wizard-like (step-based) interface, based
 * on fragments.
 * <p>
 * Each wizard step in an instance of WizardView is uniquely identifiable by its class. Therefore,
 * no two wizard steps that are direct instances of the same class can be added to an instance
 * of WizardView.
 *
 * @author lpthanh
 */
public class WizardView<ResultType> extends NoSwipeViewPager {

    private final ViewPagerAdapter<ResultType> pagerAdapter;

    private OnFinishHandler<ResultType> onFinishHandler;

    private boolean stepsAdded;

    private int scrollDuration = 1000;

    public WizardView(Context context) {
        super(context);
        this.pagerAdapter = ViewPagerAdapter.create(this);
        this.adjustTransitionSpeed();
    }

    public WizardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.pagerAdapter = ViewPagerAdapter.create(this);
        this.adjustTransitionSpeed();
    }

    /**
     * Register a handler to run when all the steps in this wizard are finished.
     */
    public void setOnFinishHandler(OnFinishHandler<ResultType> onFinishHandler) {
        this.onFinishHandler = onFinishHandler;
    }

    /**
     * Set the duration for transitioning between steps
     */
    public void setTransitionDuration(int scrollDuration) {
        this.scrollDuration = scrollDuration;
    }

    /**
     * Add the steps to the wizard, which will be identified by its class.
     * An exception will be thrown if an instance of the same class has been added previously.
     * <p>
     * This method can only be called once per instance of WizardView.
     */
    @SafeVarargs
    public final void setSteps(WizardStep<ResultType>... steps) {
        if (this.stepsAdded) {
            throw new IllegalStateException("Steps have already been added");
        }

        this.stepsAdded = true;
        for (WizardStep<ResultType> step : steps) {
            this.pagerAdapter.addStep(step);
        }

        this.setAdapter(pagerAdapter);
        this.showStep(pagerAdapter.getFirstStep());
    }

    public void showStep(Class<? extends WizardStep> stepClass) {
        Integer stepIndex = pagerAdapter.getFragmentIndex(stepClass);
        if (stepIndex == null) {
            throw new IllegalArgumentException("There is no step associated with class " + stepClass.getName());
        }

        setCurrentItem(stepIndex, true);
    }

    public void showStep(WizardStep<ResultType> step) {
        this.checkParent(step);
        this.showStep(step.getClass());
    }

    void onStepFinished(WizardStep<ResultType> step, ResultType result) {
        this.checkParent(step);
        WizardStep<ResultType> nextStep = step.getNextStep();
        if (nextStep == null) {
            // all steps are finished
            if (onFinishHandler != null) {
                onFinishHandler.onFinished(result);
            }

            return;
        }

        showStep(nextStep);
        nextStep.startStep(result);
    }

    private void checkParent(WizardStep<? extends ResultType> step) {
        if (step.getParent() != this) {
            throw new IllegalArgumentException("The step does not belong to this WizardView instance");
        }
    }

    public interface OnFinishHandler<ResultType> {
        void onFinished(ResultType resultType);
    }

    private void adjustTransitionSpeed() {
        try {
            Field scrollerField = ViewPager.class.getDeclaredField("mScroller");
            scrollerField.setAccessible(true);
            CustomScroller scroller = new CustomScroller(getContext());
            scrollerField.set(this, scroller);
        } catch (Exception ignored) {
        }
    }

    private static class ViewPagerAdapter<ResultType> extends FragmentPagerAdapter {

        public static <ResultType> ViewPagerAdapter<ResultType> create(WizardView<ResultType> parent) {
            Activity activity = (Activity) parent.getContext();
            if (!(activity instanceof FragmentActivity)) {
                throw new IllegalArgumentException("WizardView must be used within a FragmentActivity");
            }

            return new ViewPagerAdapter<>(parent,
                    ((FragmentActivity) activity).getSupportFragmentManager());
        }

        private final List<WizardStep<ResultType>> steps = new ArrayList<>();

        private final Map<Class<? extends WizardStep>, Integer> stepIndices = new HashMap<>();

        private final WizardView<ResultType> parent;

        private ViewPagerAdapter(WizardView<ResultType> parent, FragmentManager manager) {
            super(manager);
            this.parent = parent;
        }

        public void addStep(WizardStep<ResultType> step) {
            Class<? extends WizardStep> clazz = step.getClass();
            if (stepIndices.containsKey(clazz)) {
                throw new IllegalArgumentException("This class has already been added.");
            }

            final int nextIndex = steps.size();
            steps.add(step);
            stepIndices.put(clazz, nextIndex);
            step.setChildOf(parent, nextIndex);

            // wiring the step chain
            if (nextIndex > 0) {
                WizardStep<ResultType> lastStep = steps.get(nextIndex - 1);
                lastStep.setNextStep(step);
                step.setPreviousStep(lastStep);
            }
        }

        @Override
        public int getCount() {
            return steps.size();
        }

        @Override
        public Fragment getItem(int position) {
            return steps.get(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return steps.get(position).getTitle();
        }

        public Integer getFragmentIndex(Class<? extends WizardStep> clazz) {
            return stepIndices.get(clazz);
        }

        public WizardStep<ResultType> getFirstStep() {
            return steps.isEmpty() ? null : steps.get(0);
        }
    }

    /**
     * A custom implementation to adjust the scroll duration
     */
    private class CustomScroller extends Scroller {

        public CustomScroller(Context context) {
            super(context);
        }

        public CustomScroller(Context context, Interpolator interpolator) {
            super(context, interpolator);
        }

        public CustomScroller(Context context, Interpolator interpolator, boolean flywheel) {
            super(context, interpolator, flywheel);
        }


        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            super.startScroll(startX, startY, dx, dy, scrollDuration);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy) {
            super.startScroll(startX, startY, dx, dy, scrollDuration);
        }
    }
}

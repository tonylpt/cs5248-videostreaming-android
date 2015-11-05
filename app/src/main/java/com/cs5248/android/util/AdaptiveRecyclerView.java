package com.cs5248.android.util;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

/**
 * A simple RecyclerView subclass that supports providing an empty view.
 *
 * @author lpthanh
 */
public class AdaptiveRecyclerView extends RecyclerView {

    private View emptyView;

    private final AdapterDataObserver dataObserver = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            updateEmptyView();
        }
    };

    public AdaptiveRecyclerView(Context context) {
        super(context);
    }

    public AdaptiveRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AdaptiveRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Designate a view as the empty view. When the backing adapter has no
     * data this view will be made visible and the recycler view hidden.
     */
    public void setEmptyView(View emptyView) {
        this.emptyView = emptyView;
    }

    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {
        if (getAdapter() != null) {
            getAdapter().unregisterAdapterDataObserver(dataObserver);
        }

        if (adapter != null) {
            adapter.registerAdapterDataObserver(dataObserver);
        }

        super.setAdapter(adapter);
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (emptyView != null && getAdapter() != null) {
            boolean showEmptyView = getAdapter().getItemCount() == 0;
            emptyView.setVisibility(showEmptyView ? VISIBLE : GONE);
            setVisibility(showEmptyView ? GONE : VISIBLE);
        }
    }
}

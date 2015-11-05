package com.cs5248.android.ui;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.cs5248.android.R;
import com.cs5248.android.dagger.ApplicationComponent;
import com.cs5248.android.util.AdaptiveRecyclerView;
import com.cs5248.android.util.BaseFragment;

import butterknife.Bind;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * @author lpthanh
 */
public class VideoListFragment extends BaseFragment {

    @Bind(R.id.video_list_view)
    AdaptiveRecyclerView recyclerView;

    @Bind(R.id.empty_view)
    View emptyView;

    @Setter
    @Getter
    private String title;

    @Setter
    @Getter(AccessLevel.PRIVATE)
    private RecyclerView.Adapter<?> adapter;

    private OnScrollChangeListener onScrollChangeListener;

    @Override
    public void initView(View view, Bundle savedInstanceState) {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity().getBaseContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setEmptyView(emptyView);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                updateScrollListener();
            }
        });
    }

    public void setOnScrollChangeListener(OnScrollChangeListener onScrollChangeListener) {
        this.onScrollChangeListener = onScrollChangeListener;
        this.updateScrollListener();
    }

    private void updateScrollListener() {
        if (onScrollChangeListener != null && recyclerView != null) {
            onScrollChangeListener.onScrollChanged(this);
        }
    }

    @Override
    protected void injectFragment(ApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_vod;
    }

    public int getScrollPosition() {
        if (recyclerView == null) {
            return -1;
        }

        return recyclerView.computeVerticalScrollOffset();
    }

    public interface OnScrollChangeListener {
        void onScrollChanged(VideoListFragment owner);
    }

}

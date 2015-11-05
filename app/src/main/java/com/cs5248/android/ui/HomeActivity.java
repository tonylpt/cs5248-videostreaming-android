package com.cs5248.android.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.cs5248.android.R;
import com.cs5248.android.adapter.LiveAdapter;
import com.cs5248.android.adapter.VideoAdapter;
import com.cs5248.android.adapter.VodAdapter;
import com.cs5248.android.dagger.ApplicationComponent;
import com.cs5248.android.model.Video;
import com.cs5248.android.service.StreamingService;
import com.cs5248.android.util.BaseActivity;
import com.cs5248.android.util.Util;
import com.ogaclejapan.smarttablayout.SmartTabLayout;

import net.steamcrafted.materialiconlib.MaterialDrawableBuilder;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.OnClick;
import in.srain.cube.views.ptr.PtrClassicFrameLayout;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class HomeActivity extends BaseActivity {

    @Inject
    StreamingService streamingService;

    @Bind(R.id.appbar)
    AppBarLayout appBar;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.ptr_container)
    PtrClassicFrameLayout ptrContainer;

    @Bind(R.id.container)
    ViewPager viewPager;

    @Bind(R.id.record_button)
    FloatingActionButton recordButton;

    @Bind(R.id.tabs)
    SmartTabLayout tabLayout;

    private VideoListFragment vodFragment = new VideoListFragment();

    private VideoListFragment liveFragment = new VideoListFragment();

    private VideoAdapter vodAdapter = new VodAdapter();

    private VideoAdapter liveAdapter = new LiveAdapter();

    private ViewPagerAdapter pagerAdapter;


    @Override
    protected void initActivity(Bundle savedInstanceState) {
        setSupportActionBar(toolbar);

        // set up the fragments and the tabs
        vodFragment.setTitle(getString(R.string.tab_video_on_demand));
        liveFragment.setTitle(getString(R.string.tab_live_stream));

        vodFragment.setAdapter(vodAdapter);
        liveFragment.setAdapter(liveAdapter);

        ViewPagerAdapter adapter = this.pagerAdapter =
                new ViewPagerAdapter(getSupportFragmentManager(), vodFragment, liveFragment);

        viewPager.setAdapter(adapter);
        tabLayout.setViewPager(viewPager);

        // set up the record floating button
        Drawable videoIcon = MaterialDrawableBuilder.with(this)
                .setIcon(MaterialDrawableBuilder.IconValue.VIDEO)
                .setColor(Color.WHITE)
                .setToActionbarSize()
                .build();
        recordButton.setImageDrawable(videoIcon);

        // set up the pull to refresh frame
        ptrContainer.disableWhenHorizontalMove(true);

        // miscellaneous handlers to update pull-to-refresh state
        ptrContainer.setPtrHandler(new PullToRefresh());
        viewPager.addOnPageChangeListener(new ViewPagerChangeHandler());
        appBar.addOnOffsetChangedListener(new AppBarOffsetChangeHandler());

        vodAdapter.setOnItemClickListener(this::openVod);
        liveAdapter.setOnItemClickListener(this::openLive);

        updatePullToRefreshEnabled();
    }

    private void openLive(Video video, int position) {
        Intent intent = new Intent(HomeActivity.this, LiveStreamingActivity.class);
        startActivity(intent);
    }

    private void openVod(Video video, int position) {
        Intent intent = new Intent(HomeActivity.this, VodPlaybackActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.record_button)
    public void record(View view) {
        Intent intent = new Intent(HomeActivity.this, RecordActivity.class);
        startActivity(intent);
    }

    private void refreshData() {
        streamingService.getOnDemandVideos()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onRefreshSuccess, this::onRefreshFailure);
    }

    private void onRefreshSuccess(Collection<Video> videos) {
        // filter videos into live and on-demand videos
        ArrayList<Video> odVideos = new ArrayList<>();
        ArrayList<Video> liveVideos = new ArrayList<>();

        for (Video video : videos) {
            switch (video.getType()) {
                case VOD:
                    odVideos.add(video);
                    break;
                case LIVE:
                default:
                    liveVideos.add(video);
                    break;
            }
        }

        ptrContainer.setEnabled(false);
        ptrContainer.refreshComplete();
        vodAdapter.setItems(odVideos);
        liveAdapter.setItems(liveVideos);
        Snackbar.make(ptrContainer,
                getString(R.string.text_video_refresh_success, videos.size()),
                Snackbar.LENGTH_SHORT).show();
    }

    private void onRefreshFailure(Throwable throwable) {
        ptrContainer.refreshComplete();
        Util.showErrorMessage(this, getString(R.string.text_video_refresh_error), throwable);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                Intent intent = new Intent(HomeActivity.this, AboutActivity.class);
                startActivity(intent);
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
        return R.layout.activity_main;
    }

    /**
     * Check the appbar and the fragments to see if pull-to-refresh behavior can be enabled
     */
    private void updatePullToRefreshEnabled() {
        boolean enabled = true;

        // get the appBarOffset if available
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) appBar.getLayoutParams();
        AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
        if (behavior != null && behavior.getTopAndBottomOffset() != 0) {
            enabled = false;
        }

        // get the current fragment scroll position if available
        int currentFragmentIndex = viewPager.getCurrentItem();
        if (currentFragmentIndex >= 0) {
            VideoListFragment fragment = (VideoListFragment) pagerAdapter.getItem(currentFragmentIndex);
            if (fragment.getScrollPosition() != 0) {
                enabled = false;
            }
        }

        ptrContainer.setEnabled(enabled);
    }

    private class ViewPagerAdapter extends FragmentStatePagerAdapter
            implements VideoListFragment.OnScrollChangeListener {

        private final VideoListFragment[] fragments;

        public ViewPagerAdapter(FragmentManager fm, VideoListFragment... fragments) {
            super(fm);
            this.fragments = fragments;

            // setup the scroll change listener
            for (VideoListFragment fragment : fragments) {
                fragment.setOnScrollChangeListener(this);
            }
        }

        @Override
        public Fragment getItem(int position) {
            return fragments[position];
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
        }

        @Override
        public int getCount() {
            return fragments.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return fragments[position].getTitle();
        }

        @Override
        public void onScrollChanged(VideoListFragment owner) {
            updatePullToRefreshEnabled();
        }
    }

    private class PullToRefresh implements PtrHandler {
        @Override
        public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
            return true;
        }

        @Override
        public void onRefreshBegin(PtrFrameLayout frame) {
            refreshData();
        }
    }

    private class ViewPagerChangeHandler implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageSelected(int position) {
            updatePullToRefreshEnabled();
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    }

    private class AppBarOffsetChangeHandler implements AppBarLayout.OnOffsetChangedListener {

        @Override
        public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
            updatePullToRefreshEnabled();
        }
    }

}

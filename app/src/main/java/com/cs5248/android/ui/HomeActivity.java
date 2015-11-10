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
import com.cs5248.android.model.cache.VideoListCache;
import com.cs5248.android.service.ApiService;
import com.cs5248.android.util.BaseActivity;
import com.cs5248.android.util.Util;
import com.ogaclejapan.smarttablayout.SmartTabLayout;

import net.steamcrafted.materialiconlib.MaterialDrawableBuilder;

import org.parceler.Parcels;

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
import timber.log.Timber;

public class HomeActivity extends BaseActivity {

    private static final int REQUEST_RECORD_VIDEO = 1;

    private static final int REQUEST_OPEN_VOD = 2;

    private static final int REQUEST_OPEN_LIVE = 3;

    @Inject
    ApiService apiService;

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
        loadVideos();
    }

    private void openLive(Video video, int position) {
        Intent intent = new Intent(HomeActivity.this, LiveStreamingActivity.class);
        intent.putExtra("video", Parcels.wrap(video));
        startActivityForResult(intent, REQUEST_OPEN_LIVE);
    }

    private void openVod(Video video, int position) {
        Intent intent = new Intent(HomeActivity.this, VodPlaybackActivity.class);
        intent.putExtra("video", Parcels.wrap(video));
        startActivityForResult(intent, REQUEST_OPEN_VOD);
    }

    @OnClick(R.id.record_button)
    public void record(View view) {
        Intent intent = new Intent(HomeActivity.this, RecordActivity.class);
        startActivityForResult(intent, REQUEST_RECORD_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_RECORD_VIDEO:
                if (resultCode == RESULT_OK) {
                    loadVideosFromServer();
                }
                break;
            case REQUEST_OPEN_LIVE:
                break;
            case REQUEST_OPEN_VOD:
                break;
        }
    }

    /**
     * Load the videos temporarily from cache. And then try to load from server.
     */
    private void loadVideos() {
        // first try to load from cache
        Collection<Video> cached = VideoListCache.load();
        if (cached != null) {
            loadVideosIntoUI(cached);
        }

        // then try to load from server
        ptrContainer.autoRefresh(true);
    }

    private void loadVideosFromServer() {
        Timber.d("Start loading videos from server");

        apiService.getOnDemandVideos()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onLoadingFromServerSuccess, this::onLoadingFromServerFailure);
    }

    private void onLoadingFromServerSuccess(Collection<Video> videos) {
        Timber.d("Successfully loaded %d items from server", videos.size());

        VideoListCache.update(videos);
        loadVideosIntoUI(videos);

        // notify successful
        Snackbar.make(ptrContainer,
                getString(R.string.text_video_refresh_success, videos.size()),
                Snackbar.LENGTH_SHORT).show();
    }

    private void onLoadingFromServerFailure(Throwable throwable) {
        Timber.e(throwable, "Error loading data from server");

        ptrContainer.refreshComplete();
        Util.showErrorMessage(this, getString(R.string.text_video_refresh_error), throwable);
    }

    private void loadVideosIntoUI(Collection<Video> videos) {
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
            loadVideosFromServer();
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

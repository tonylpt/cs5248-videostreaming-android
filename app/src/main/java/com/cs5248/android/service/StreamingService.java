package com.cs5248.android.service;

import android.content.Context;

import com.cs5248.android.model.Video;
import com.cs5248.android.model.VideoStatus;
import com.cs5248.android.model.VideoType;
import com.cs5248.android.model.cache.IgnoreAAModelIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.JacksonConverter;
import rx.Observable;

/**
 * This classes encapsulate the core API, and may provide other features such as adaptation between
 * server and client models, caching or network request intercepting.
 *
 * @author lpthanh
 */
public class StreamingService {

    private static final String WEB_SERVICE_BASE_URL = "http://192.168.0.177:5000";

    private static final long TIME_OUT = 20000;

    private final Api api;

    private final Context context;

    public StreamingService(Context context) {
        this.context = context;

        RequestInterceptor requestInterceptor = request -> {
            request.addHeader("Accept", "application/json");
        };

        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(new PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy());
        mapper.setAnnotationIntrospector(new IgnoreAAModelIntrospector());

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(WEB_SERVICE_BASE_URL)
                .setConverter(new JacksonConverter(mapper))
                .setRequestInterceptor(requestInterceptor)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        api = restAdapter.create(Api.class);
    }

    public Api getApi() {
        return this.api;
    }

    public Observable<List<Video>> getOnDemandVideos() {
        return getApi()
                .getOnDemandVideos()
                .timeout(TIME_OUT, TimeUnit.MILLISECONDS);
    }

    public Observable<List<Video>> getLiveStreams() {
        return getApi()
                .getLiveStreams()
                .timeout(TIME_OUT, TimeUnit.MILLISECONDS);
    }

    public Observable<Recording> createNewRecording(String title) {
        Video video = new Video();
        video.setTitle(title);
        video.setCreatedAt(new Date());
        video.setStatus(VideoStatus.EMPTY);
        video.setType(VideoType.LIVE);

        return getApi()
                .createVideo(video)
                .timeout(TIME_OUT, TimeUnit.MILLISECONDS)
                .map(RecordingImpl::new);
    }

    private class RecordingImpl implements Recording {

        private Video video;

        public RecordingImpl(Video video) {
            this.video = video;
        }

    }

}

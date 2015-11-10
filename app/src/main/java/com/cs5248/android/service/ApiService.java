package com.cs5248.android.service;

import android.content.Context;

import com.cs5248.android.model.Video;
import com.cs5248.android.model.VideoSegment;
import com.cs5248.android.model.cache.IgnoreAAModelIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.squareup.okhttp.OkHttpClient;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.JacksonConverter;
import retrofit.mime.TypedFile;
import rx.Observable;

/**
 * This classes encapsulate the core API, and may provide other features such as adaptation between
 * server and client models, caching or network request intercepting.
 *
 * @author lpthanh
 */
public class ApiService {

    //    private static final String WEB_SERVICE_BASE_URL = "http://tr-03155248.cloudapp.net";
    private static final String WEB_SERVICE_BASE_URL = "http://192.168.0.130:5000";

    private static final long TIME_OUT = 20000;

    private final Api api;

    private final Context context;

    public ApiService(Context context) {
        this.context = context;

        RequestInterceptor requestInterceptor = request -> {
            request.addHeader("Cache-control", "public,max-age=0");
            request.addHeader("Accept", "application/json");
        };

        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(new PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy());
        mapper.setAnnotationIntrospector(new IgnoreAAModelIntrospector());

        final OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setReadTimeout(TIME_OUT, TimeUnit.MILLISECONDS);
        okHttpClient.setConnectTimeout(TIME_OUT, TimeUnit.MILLISECONDS);

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(WEB_SERVICE_BASE_URL)
                .setClient(new OkClient(okHttpClient))
                .setConverter(new JacksonConverter(mapper))
                .setRequestInterceptor(requestInterceptor)
                .setLogLevel(RestAdapter.LogLevel.BASIC)
                .build();

        api = restAdapter.create(Api.class);
    }

    public Api getApi() {
        return this.api;
    }

    /**
     * Query all VODs asynchronously.
     */
    public Observable<List<Video>> getOnDemandVideos() {
        return getApi()
                .getOnDemandVideos()
                .timeout(TIME_OUT, TimeUnit.MILLISECONDS);
    }

    /**
     * Query all live streams asynchronously.
     */
    public Observable<List<Video>> getLiveStreams() {
        return getApi()
                .getLiveStreams()
                .timeout(TIME_OUT, TimeUnit.MILLISECONDS);
    }

    /**
     * Create a new video asynchronously.
     */
    Observable<Video> createVideo(Video video) {
        return getApi()
                .createVideo(video)
                .timeout(TIME_OUT, TimeUnit.MILLISECONDS);
    }

    /**
     * Upload a segment synchronously.
     */
    public VideoSegment uploadSegment(VideoSegment input) throws SegmentUploadException {
        File file = new File(input.getOriginalPath());
        if (!file.exists() || !file.isFile()) {
            throw new SegmentUploadException("File does not exist: " + file.getAbsolutePath(), input);
        }

        TypedFile typedFile = new TypedFile("multipart/form-data", file);

        return getApi().createSegment(input.getVideoId(),
                input.getSegmentId(),
                input.getOriginalExtension(),
                typedFile);
    }

    /**
     * Mark the video end synchronously.
     */
    public Video markVideoUploadEnd(Long videoId, Long lastSegmentId) {
        return getApi().signalVideoEnd(videoId, lastSegmentId);
    }

}


package com.cs5248.android.service;

import android.content.Context;
import android.util.Pair;

import com.cs5248.android.Config;
import com.cs5248.android.model.Video;
import com.cs5248.android.model.VideoSegment;
import com.cs5248.android.model.cache.IgnoreAAModelIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.squareup.okhttp.OkHttpClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.Header;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.converter.JacksonConverter;
import retrofit.mime.TypedFile;
import retrofit.mime.TypedInput;
import rx.Observable;
import timber.log.Timber;

/**
 * This classes encapsulate the core API, and may provide other features such as adaptation between
 * server and client models, caching or network request intercepting.
 *
 * @author lpthanh
 */
public class ApiService {

    private static final long TIME_OUT = 20000;

    private final Api api;

    private final Context context;

    public ApiService(Context context) {
        this.context = context;

        String serverBaseUrl = Config.SERVER_BASE_URL;
        long timeout = TIME_OUT;

        RequestInterceptor requestInterceptor = request -> {
            request.addHeader("Cache-control", "public,max-age=0");
            request.addHeader("Accept", "application/json");
        };

        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(new PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy());
        mapper.setAnnotationIntrospector(new IgnoreAAModelIntrospector());

        final OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setReadTimeout(timeout, TimeUnit.MILLISECONDS);
        okHttpClient.setConnectTimeout(timeout, TimeUnit.MILLISECONDS);

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(serverBaseUrl)
                .setClient(new OkClient(okHttpClient))
                .setConverter(new JacksonConverter(mapper))
                .setRequestInterceptor(requestInterceptor)
                .setLogLevel(RestAdapter.LogLevel.BASIC)
                .build();

        api = restAdapter.create(Api.class);

        Timber.d("Retrofit was setup to connect to %s, with timeout=%dms", serverBaseUrl, timeout);
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

    /**
     * Read the MPD for the video. If lastSegmentId is not null, the MPD only includes
     * the segments after the lastSegmentId (exclusive).
     *
     * @return a tuple: (InputStream, tuple(last segment index (of the returned MPD), stream ended))
     */
    public Pair<InputStream, Pair<Long, Boolean>> streamMpd(Long videoId, Long lastSegmentId) {
        try {
            Response response = getApi().streamMPD(videoId, lastSegmentId);
            InputStream stream = response.getBody().in();
            Long newLastSegmentId = null;
            boolean streamEnded = false;

            for (Header header : response.getHeaders()) {
                if (header.getName().equals("lastSegmentId")) {
                    try {
                        newLastSegmentId = Long.parseLong(header.getValue());
                    } catch (Exception e) {
                        Timber.e(e, "Error parsing the lastSegmentId header from server.");
                    }
                } else if (header.getName().equals("streamEnded")) {
                    try {
                        streamEnded = Boolean.parseBoolean(header.getValue());
                    } catch (Exception e) {
                        Timber.e(e, "Error parsing the streamEnded header from server.");
                    }
                }
            }
            return new Pair<>(stream, new Pair<>(newLastSegmentId, streamEnded));
        } catch (Exception e) {
            Timber.e(e, "Error streaming MPD from server.");
            return null;
        }
    }

    /**
     * Stream a file from the server as an InputStream.
     *
     * @return a tuple of InputStream and the content length
     */
    public Pair<InputStream, Long> streamVideoFile(String path) throws IOException {
        Response response = getApi().streamVideo(path);
        TypedInput body = response.getBody();
        InputStream stream = body.in();
        long length = body.length();

        return new Pair<>(stream, length);
    }

}


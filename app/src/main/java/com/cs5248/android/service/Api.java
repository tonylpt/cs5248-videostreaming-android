package com.cs5248.android.service;

import com.cs5248.android.model.Video;
import com.cs5248.android.model.VideoSegment;

import java.util.List;

import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import rx.Observable;

/**
 * @author lpthanh
 */
public interface Api {

    @POST("/videos")
    Observable<Video> createVideo(@Body Video video);

    @GET("/videos")
    Observable<List<Video>> getOnDemandVideos();

    @GET("/livestreams")
    Observable<List<Video>> getLiveStreams();

    @GET("/segments/{videoId}")
    Observable<List<VideoSegment>> getVideoSegments(@Path("videoId") String videoId);

}

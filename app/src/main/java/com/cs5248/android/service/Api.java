package com.cs5248.android.service;

import com.cs5248.android.model.Video;
import com.cs5248.android.model.VideoSegment;

import java.util.List;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.mime.TypedFile;
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

    @Multipart
    @POST("/video_segment/{video_id}")
    void createSegment(@Path("video_id") Long videoId,
                       @Part("segment_id") Long segmentId,
                       @Part("original_extension") String extension,
                       @Part("data") TypedFile segmentFile,
                       Callback<VideoSegment> callback);

    @FormUrlEncoded
    @POST("/video_end/{video_id}")
    void signalVideoEnd(@Path("video_id") Long videoId,
                        @Field("last_segment_id") Long lastSegmentId,
                        Callback<Video> callback);

}

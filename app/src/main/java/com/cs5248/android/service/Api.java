package com.cs5248.android.service;

import com.cs5248.android.Config;
import com.cs5248.android.model.Video;
import com.cs5248.android.model.VideoSegment;

import java.util.List;

import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.http.Streaming;
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

    /**
     * Since this will be executed within a job queue, it does not need to be async.
     */
    @Multipart
    @POST("/video_segment/{video_id}")
    VideoSegment createSegment(@Path("video_id") Long videoId,
                               @Part("segment_id") Long segmentId,
                               @Part("original_extension") String extension,
                               @Part("data") TypedFile segmentFile);

    @FormUrlEncoded
    @POST("/video_end/{video_id}")
    Video signalVideoEnd(@Path("video_id") Long videoId,
                         @Field("last_segment_id") Long lastSegmentId);

    @GET("/live_mpd/{video_id}.mpd")
    @Streaming
    Response streamMPD(@Path("video_id") Long videoId,
                       @Query("last_segment_id") Long lastSegmentId);

    @GET(Config.VIDEO_FILES_PREFIX + "{path}")
    @Streaming
    Response streamVideo(@Path(value = "path", encode = false) String path);

}

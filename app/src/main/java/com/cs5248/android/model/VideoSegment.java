package com.cs5248.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * @author lpthanh
 */
public class VideoSegment {

    @Getter
    @Setter
    private Long videoId;

    @Getter
    @Setter
    private Long segmentId;

    @Getter
    @Setter
    private String originalPath;

    @Getter
    @Setter
    private String originalExtension;

    @Getter
    @Setter
    private String mediaMpd;

    @Getter
    @Setter
    private String mediaM3u8;

    @Getter
    @Setter
    private VideoSegmentStatus status;

    @Getter
    @Setter
    @JsonProperty("repr_1_status")
    private VideoSegmentStatus reprStatus1;


    @Getter
    @Setter
    @JsonProperty("repr_2_status")
    private VideoSegmentStatus reprStatus2;


    @Getter
    @Setter
    @JsonProperty("repr_3_status")
    private VideoSegmentStatus reprStatus3;

}

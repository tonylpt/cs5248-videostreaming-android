package com.cs5248.android.model;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.parceler.Parcel;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author lpthanh
 */
@ToString
@Table(name = "videos", id = "id")
@Parcel(value = Parcel.Serialization.FIELD, analyze = Video.class) // do not analyze superclass
public class Video extends Model {

    @Getter
    @Setter
    @Column(name = "video_id", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    private Long videoId;

    @Getter
    @Setter
    @Column
    private String title;

    @Getter
    @Setter
    @Column
    private VideoStatus status;

    @Getter
    @Setter
    @Column
    private VideoType type;

    @Setter
    @Getter
    @Column
    private Date createdAt;

    @Setter
    @Getter
    @Column
    private long segmentCount;

    @Setter
    @Getter
    @Column
    private long segmentDuration;

    @Setter
    @Getter
    @Column
    @JsonProperty("repr_1_name")
    private String repr1Name;
    
    @Setter
    @Getter
    @Column
    @JsonProperty("repr_1_bandwidth")
    private String repr1Bandwidth;

    @Setter
    @Getter
    @Column
    @JsonProperty("repr_1_width")
    private String repr1Width;

    @Setter
    @Getter
    @Column
    @JsonProperty("repr_1_height")
    private String repr1Height;

    @Setter
    @Getter
    @Column
    @JsonProperty("repr_2_name")
    private String repr2Name;

    @Setter
    @Getter
    @Column
    @JsonProperty("repr_2_bandwidth")
    private String repr2Bandwidth;

    @Setter
    @Getter
    @Column
    @JsonProperty("repr_2_width")
    private String repr2Width;

    @Setter
    @Getter
    @Column
    @JsonProperty("repr_2_height")
    private String repr2Height;

    @Setter
    @Getter
    @Column
    @JsonProperty("repr_3_name")
    private String repr3Name;

    @Setter
    @Getter
    @Column
    @JsonProperty("repr_3_bandwidth")
    private String repr3Bandwidth;

    @Setter
    @Getter
    @Column
    @JsonProperty("repr_3_width")
    private String repr3Width;

    @Setter
    @Getter
    @Column
    @JsonProperty("repr_3_height")
    private String repr3Height;
    
    @Setter
    @Getter
    @Column
    @JsonProperty("uri_mpd")
    private String uriMpd;

    @Setter
    @Getter
    @Column
    @JsonProperty("uri_m3u8")
    private String uriM3u8;

}

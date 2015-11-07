package com.cs5248.android.model;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.parceler.Parcel;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * @author lpthanh
 */
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
    @JsonProperty("repr_1_id")
    private String reprId1;

    @Setter
    @Getter
    @Column
    @JsonProperty("repr_2_id")
    private String reprId2;

    @Setter
    @Getter
    @Column
    @JsonProperty("repr_3_id")
    private String reprId3;

    @Setter
    @Getter
    @Column
    private String uriMpd;

    @Setter
    @Getter
    @Column
    private String uriM3u8;

}

package com.cs5248.android.model;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author lpthanh
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class VideoSegment {

    @Getter
    @Setter
    private Long videoId;

    @Getter
    @Setter
    private long sequenceIndex;

    @Getter
    @Setter
    private List<Byte> videoData;

}

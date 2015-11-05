package com.cs5248.android.model;

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
    private String videoId;

    @Getter
    @Setter
    private long sequenceIndex;

}

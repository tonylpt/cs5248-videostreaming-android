package com.cs5248.android.model;

import android.content.Context;

/**
 * @author lpthanh
 */
public class StreamingManager {

    private final Context context;

    public StreamingManager(Context context) {
        this.context = context;
    }

    public void testModel() {
        Video video = new Video();
        video.setTitle("hahahaa");
    }
}

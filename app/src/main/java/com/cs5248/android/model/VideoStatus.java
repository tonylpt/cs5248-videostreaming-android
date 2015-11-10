package com.cs5248.android.model;

/**
 * @author lpthanh
 */
public enum VideoStatus {

    /**
     * Just created. No content yet.
     */
    EMPTY,

    /**
     * The video is being uploaded.
     */
    UPLOADING,

    /**
     * Successfully uploaded and processed.
     */
    OK,

    /**
     * Failure during upload / processing
     */
    ERROR

}

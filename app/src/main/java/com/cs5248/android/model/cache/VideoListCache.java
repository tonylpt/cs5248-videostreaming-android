package com.cs5248.android.model.cache;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.cs5248.android.model.Video;

import java.util.Collection;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * This class is for caching the list of videos in the local database for faster access / better
 * user experience.
 *
 * @author lpthanh
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VideoListCache {

    /**
     * @return a list of video from cache, or null if an error has occurred
     */
    public static Collection<Video> load() {
        try {
            return new Select()
                    .from(Video.class)
                    .execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return whether the update was successful or not
     */
    public static boolean update(Collection<Video> videos) {
        try {
            ActiveAndroid.beginTransaction();
            try {
                // clear the current list
                new Delete().from(Video.class).execute();

                // update with the new list
                for (Video video : videos) {
                    video.save();
                }

                ActiveAndroid.setTransactionSuccessful();
                return true;
            } finally {
                ActiveAndroid.endTransaction();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

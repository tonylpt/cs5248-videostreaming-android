package com.cs5248.android.adapter;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.cs5248.android.Config;
import com.cs5248.android.R;
import com.cs5248.android.model.Video;
import com.cs5248.android.model.VideoStatus;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;

import at.markushi.ui.CircleButton;
import butterknife.Bind;

/**
 * @author lpthanh
 */
public class VodAdapter extends VideoAdapter {

    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("MMM dd, yyyy  KK:ss a");

    public VodAdapter(Context context) {
        super(context);
    }

    @Override
    protected int getItemLayoutId() {
        return R.layout.item_vod;
    }

    @Override
    protected VideoAdapter.VideoViewHolder createViewHolder(View view) {
        return new ViewHolder(view);
    }

    protected class ViewHolder extends VideoAdapter.VideoViewHolder {

        @Bind(R.id.previewer)
        ImageView previewer;

        @Bind(R.id.status_display)
        CircleButton statusDisplay;

        @Bind(R.id.id_text)
        TextView idText;

        @Bind(R.id.title_text)
        TextView titleText;

        @Bind(R.id.date_text)
        TextView dateText;

        public ViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        protected void display(Video video) {
            idText.setText(String.valueOf(video.getVideoId()));
            titleText.setText(video.getTitle());
            dateText.setText(dateFormatter.format(video.getCreatedAt()));
            statusDisplay.setVisibility(video.getStatus() == VideoStatus.ERROR ? View.VISIBLE : View.INVISIBLE);

            // load the video thumbnail
            String thumbnailUri = video.getUriThumbnail();
            if (thumbnailUri != null && thumbnailUri.length() > 0) {
                Uri thumbnail = Uri.parse(Config.SERVER_BASE_URL)
                        .buildUpon()
                        .path(video.getBaseUrl())
                        .appendPath(video.getUriThumbnail())
                        .build();

                Picasso.with(getContext()).load(thumbnail).into(previewer);
            } else {
                previewer.setImageDrawable(null);
            }
        }
    }
}

package com.cs5248.android.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cs5248.android.model.Video;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import butterknife.ButterKnife;

/**
 * @author lpthanh
 */
public abstract class VideoAdapter<VH extends VideoAdapter.VideoViewHolder>
        extends RecyclerView.Adapter<VH> {

    private final Context context;

    private List<Video> videos = new ArrayList<>();

    private OnItemClickListener onItemClickListener;

    public VideoAdapter(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public final void setItems(Collection<Video> items) {
        videos.clear();
        videos.addAll(items);
        notifyDataSetChanged();
    }

    public final void setOnItemClickListener(final OnItemClickListener itemClickListener) {
        this.onItemClickListener = itemClickListener;
    }

    @Override
    public final VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(getItemLayoutId(), parent, false);
        return createViewHolder(view);
    }

    @Override
    public final void onBindViewHolder(VH holder, int position) {
        holder.display(videos.get(position));
    }

    @Override
    public final int getItemCount() {
        return videos.size();
    }

    private void onItemClick(int position) {
        if (onItemClickListener != null) {
            onItemClickListener.onItemClicked(videos.get(position), position);
        }
    }

    protected abstract int getItemLayoutId();

    protected abstract VH createViewHolder(View view);

    public interface OnItemClickListener {
        void onItemClicked(Video video, int position);
    }

    protected abstract class VideoViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        public VideoViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        protected abstract void display(Video video);

        @Override
        public final void onClick(View v) {
            if (v != this.itemView) {
                return;
            }

            onItemClick(getAdapterPosition());
        }
    }
}

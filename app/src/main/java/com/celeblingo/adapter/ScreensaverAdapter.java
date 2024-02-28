package com.celeblingo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.celeblingo.R;

import java.util.ArrayList;

public class ScreensaverAdapter extends RecyclerView.Adapter<ScreensaverAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<String> imageUrls;
    private final OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public ScreensaverAdapter(Context context, ArrayList<String> imageUrls, OnItemClickListener onItemClickListener) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ScreensaverAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(
                R.layout.item_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScreensaverAdapter.ViewHolder holder, int position) {
        Glide.with(context).load(imageUrls.get(position)).into(holder.imageView);
        holder.imageView.setOnClickListener(view -> {
            onItemClickListener.onItemClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewItem);
        }
    }
}

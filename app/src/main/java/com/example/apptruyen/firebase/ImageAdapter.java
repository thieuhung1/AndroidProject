package com.example.apptruyen.firebase;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.apptruyen.R;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private final List<String> imageUrls;

    public ImageAdapter(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate layout item_comic_image.xml chứa ImageView
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comic_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);

        // Dùng Glide để load ảnh
        Glide.with(holder.imageView.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image) // ảnh hiện khi loading
                .error(R.drawable.error_image)             // ảnh hiện khi lỗi load
                .diskCacheStrategy(DiskCacheStrategy.ALL)  // cache tất cả ảnh
                .thumbnail(0.25f)                          // load ảnh kích thước nhỏ trước
                .fitCenter()                               // giữ tỉ lệ ảnh
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size(); // Không cần kiểm tra null
    }

    // ViewHolder chứa ImageView để tái sử dụng views hiệu quả
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}
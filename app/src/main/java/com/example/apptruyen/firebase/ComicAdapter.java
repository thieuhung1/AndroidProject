package com.example.apptruyen.firebase;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.apptruyen.Home.ChapterDetail;
import com.example.apptruyen.R;
import com.example.apptruyen.model.Comic;

import java.util.List;

public class ComicAdapter extends RecyclerView.Adapter<ComicAdapter.ComicViewHolder> {

    private static final String TAG = "ComicAdapter";
    private Context context;
    private List<Comic> comics;

    public ComicAdapter(Context context, List<Comic> comics) {
        this.context = context;
        this.comics = comics;
    }

    @NonNull
    @Override
    public ComicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comic_vertical, parent, false);
        return new ComicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComicViewHolder holder, int position) {
        try {
            Comic comic = comics.get(position);
            holder.textName.setText(comic.name+comic._id );
            holder.textStatus.setText(comic.status);

            // Cải thiện cách load hình ảnh với Glide
            String imageUrl = "https://img.otruyenapi.com/uploads/comics/" + comic.thumb_url;
            RequestOptions options = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .override(300, 450); // Giới hạn kích thước hình ảnh

            Glide.with(context)
                    .load(imageUrl)
                    .apply(options)
                    .into(holder.imageThumb);

            holder.itemView.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(context, ChapterDetail.class);
                    intent.putExtra("comicId", comic._id);
                    intent.putExtra("slug", comic.slug);// Truyền ID thay vì cả object
                    context.startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error opening comic details", e);
                    Toast.makeText(context, "Không thể mở chi tiết truyện", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error binding view holder", e);
        }
    }

    @Override
    public int getItemCount() {
        return comics != null ? comics.size() : 0;
    }

    public static class ComicViewHolder extends RecyclerView.ViewHolder {
        ImageView imageThumb;
        TextView textName, textStatus;

        public ComicViewHolder(@NonNull View itemView) {
            super(itemView);
            imageThumb = itemView.findViewById(R.id.imageThumb);
            textName = itemView.findViewById(R.id.textName);
            textStatus = itemView.findViewById(R.id.textStatus);
        }
    }
}
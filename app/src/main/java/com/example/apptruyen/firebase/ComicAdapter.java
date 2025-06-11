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
import com.example.apptruyen.Home.ChapterDetail;
import com.example.apptruyen.R;
import com.example.apptruyen.model.Comic;

import java.util.List;

public class ComicAdapter extends RecyclerView.Adapter<ComicAdapter.ComicViewHolder> {
    private static final String TAG = "ComicAdapter";
    private final Context context;
    private final List<Comic> comics;

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
        Comic comic = comics.get(position);
        holder.textName.setText(comic.name + " (" + comic._id + ")");
        holder.textStatus.setText(comic.status);

        Glide.with(context)
                .load("https://img.otruyenapi.com/uploads/comics/" + comic.thumb_url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .override(300, 450)
                .into(holder.imageThumb);

        holder.itemView.setOnClickListener(v -> openComicDetails(comic));
    }

    private void openComicDetails(Comic comic) {
        Intent intent = new Intent(context, ChapterDetail.class);
        intent.putExtra("comicId", comic._id);
        intent.putExtra("slug", comic.slug);
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return comics.size();
    }

    public static class ComicViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageThumb;
        final TextView textName, textStatus;

        public ComicViewHolder(@NonNull View itemView) {
            super(itemView);
            imageThumb = itemView.findViewById(R.id.imageThumb);
            textName = itemView.findViewById(R.id.textName);
            textStatus = itemView.findViewById(R.id.textStatus);
        }
    }
}
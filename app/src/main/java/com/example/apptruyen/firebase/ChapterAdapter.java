package com.example.apptruyen.firebase;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apptruyen.R;
import com.example.apptruyen.Home.ChapterContentActivity;
import com.example.apptruyen.model.Comic;
import java.util.List;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {
    private Context context;
    private List<Comic.Chapter> chapters;

    public ChapterAdapter(List<Comic.Chapter> chapters) {
        this.chapters = chapters;
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_chapter, parent, false);
        return new ChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        Comic.Chapter chapter = chapters.get(position);
        holder.chapterName.setText(chapter.chapter_name);
        holder.chapterTitle.setText(chapter.chapter_title != null && !chapter.chapter_title.isEmpty()
                ? chapter.chapter_title : "Chapter " + chapter.chapter_name);

        // Xử lý sự kiện click vào chapter
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChapterContentActivity.class);
            intent.putExtra("chapter_api_data", chapter.chapter_api_data);
            intent.putExtra("chapter_name", chapter.chapter_name);
            intent.putExtra("chapter_title", chapter.chapter_title);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chapters != null ? chapters.size() : 0;
    }

    public void updateChapters(List<Comic.Chapter> newChapters) {
        this.chapters = newChapters;
        notifyDataSetChanged();
    }

    static class ChapterViewHolder extends RecyclerView.ViewHolder {
        TextView chapterName, chapterTitle;

        public ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            chapterName = itemView.findViewById(R.id.chapterName);
            chapterTitle = itemView.findViewById(R.id.chapterTitle);
        }
    }
}
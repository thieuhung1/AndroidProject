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
import android.util.Log;
import android.widget.Toast;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {
    private static final String TAG = "ChapterAdapter";
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
                ? chapter.chapter_title : "Chương " + chapter.chapter_name);

        holder.itemView.setOnClickListener(v -> {
            String chapterId = extractChapterId(chapter.chapter_api_data);
            if (chapterId == null) {
                Log.e(TAG, "URL chapter_api_data không hợp lệ: " + chapter.chapter_api_data);
                Toast.makeText(context, "Dữ liệu chương không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(context, ChapterContentActivity.class);
            intent.putExtra("chapter_id", chapterId);
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

    private String extractChapterId(String chapterApiData) {
        if (chapterApiData == null || chapterApiData.isEmpty()) {
            Log.e(TAG, "chapter_api_data là null hoặc rỗng");
            return null;
        }
        try {
            // Tách URL theo dấu '/' và lấy phần cuối
            String[] parts = chapterApiData.split("/");
            return parts[parts.length - 1];
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi trích xuất ID chương từ: " + chapterApiData, e);
            return null;
        }
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
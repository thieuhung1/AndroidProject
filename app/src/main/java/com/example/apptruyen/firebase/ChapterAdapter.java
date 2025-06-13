package com.example.apptruyen.firebase;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.apptruyen.Home.ChapterContentActivity;
import com.example.apptruyen.R;
import com.example.apptruyen.model.Comic;
import java.util.List;
import java.util.Objects;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {
    private List<Comic.Chapter> chapters;
    private long lastClickTime = 0;
    private static final long CLICK_DEBOUNCE_MS = 500; // 500ms debounce

    public ChapterAdapter(List<Comic.Chapter> chapters) {
        this.chapters = chapters != null ? chapters : List.of();
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chapter, parent, false);
        return new ChapterViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int pos) {
        Comic.Chapter c = chapters.get(pos);
        holder.chapterName.setText(c.chapter_name);
        holder.chapterTitle.setText(c.chapter_title != null && !c.chapter_title.isEmpty()
                ? c.chapter_title : "Chương " + c.chapter_name);

        holder.itemView.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime > CLICK_DEBOUNCE_MS) {
                lastClickTime = currentTime;
                String id = extractChapterId(c.chapter_api_data);
                if (id == null) {
                    Toast.makeText(v.getContext(), "Dữ liệu chương không hợp lệ", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent i = new Intent(v.getContext(), ChapterContentActivity.class);
                i.putExtra("chapter_id", id);
                i.putExtra("chapter_name", c.chapter_name);
                i.putExtra("chapter_title", c.chapter_title);
                v.getContext().startActivity(i);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chapters.size();
    }

    public void updateChapters(List<Comic.Chapter> newList) {
        this.chapters = newList != null ? newList : List.of();
        notifyDataSetChanged();
    }

    private String extractChapterId(String data) {
        try {
            String[] parts = Objects.requireNonNullElse(data, "").split("/");
            return parts.length > 0 ? parts[parts.length - 1] : null;
        } catch (Exception e) {
            return null;
        }
    }

    static class ChapterViewHolder extends RecyclerView.ViewHolder {
        TextView chapterName, chapterTitle;

        ChapterViewHolder(@NonNull View v) {
            super(v);
            chapterName = v.findViewById(R.id.chapterName);
            chapterTitle = v.findViewById(R.id.chapterTitle);
        }
    }
}
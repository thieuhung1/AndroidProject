package com.example.apptruyen.firebase;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apptruyen.R;

import java.util.List;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {

    private Context context;
    private List<String> chapters;

    public ChapterAdapter(Context context, List<String> chapters) {
        this.context = context;
        this.chapters = chapters;
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chapter, parent, false);
        return new ChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        String chapter = chapters.get(position);
        holder.textChapterTitle.setText(chapter);

        holder.itemView.setOnClickListener(v -> {
            try {
                // TODO: Thêm code để mở trang đọc chương cụ thể
                Toast.makeText(context, "Đang mở " + chapter, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(context, "Không thể mở chương", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }

    @Override
    public int getItemCount() {
        return chapters != null ? chapters.size() : 0;
    }

    public static class ChapterViewHolder extends RecyclerView.ViewHolder {
        TextView textChapterTitle;

        public ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            textChapterTitle = itemView.findViewById(R.id.textChapterTitle);
        }
    }
}
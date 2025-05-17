package com.example.apptruyen.Home;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.apptruyen.R;
import com.example.apptruyen.firebase.ChapterAdapter;
import com.example.apptruyen.model.Comic;

import java.util.ArrayList;
import java.util.List;

public class ComicDetailActivity extends AppCompatActivity {

    private static final String TAG = "ComicDetailActivity";
    private ImageView detailThumb;
    private TextView detailName, detailStatus;
    private RecyclerView chapterRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_comic_detail);

            // Khởi tạo view
            detailThumb = findViewById(R.id.detailThumb);
            detailName = findViewById(R.id.detailName);
            detailStatus = findViewById(R.id.detailStatus);
            Button readButton = findViewById(R.id.readButton);
            chapterRecyclerView = findViewById(R.id.chapterRecyclerView);

            // Cấu hình RecyclerView
            chapterRecyclerView.setLayoutManager(new LinearLayoutManager(this));

            // Lấy dữ liệu comic từ intent
            Comic comic = (Comic) getIntent().getSerializableExtra("comic");
            if (comic != null) {
                setupComicDetails(comic);
                setupChaptersList(comic);

                // Thiết lập sự kiện cho nút đọc truyện
                readButton.setOnClickListener(v -> {
                    try {
                        // Thêm code mở Activity đọc truyện ở đây
                        // Intent readIntent = new Intent(this, ReadComicActivity.class);
                        // readIntent.putExtra("comic_id", comic.id);
                        // startActivity(readIntent);
                        Toast.makeText(this, "Chức năng đọc truyện đang được phát triển", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting read activity", e);
                        Toast.makeText(this, "Có lỗi xảy ra, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Xử lý khi không có dữ liệu truyện
                Toast.makeText(this, "Không thể tải thông tin truyện", Toast.LENGTH_SHORT).show();
                finish(); // Đóng activity nếu không có dữ liệu
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Đã xảy ra lỗi", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupComicDetails(Comic comic) {
        try {
            detailName.setText(comic.name);
            detailStatus.setText(comic.status);

            // Cải thiện cách load hình ảnh với Glide
            String imageUrl = "https://img.otruyenapi.com/uploads/comics/" + comic.thumb_url;
            RequestOptions options = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .override(800, 1200) // Giới hạn kích thước hình ảnh
                    .timeout(30000); // Tăng timeout

            Glide.with(this)
                    .load(imageUrl)
                    .apply(options)
                    .into(detailThumb);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up comic details", e);
        }
    }

    private void setupChaptersList(Comic comic) {
        try {
            // Tạo danh sách chương mẫu (bạn sẽ thay thế bằng dữ liệu thực tế)
            List<String> chapterList = new ArrayList<>();
            if (comic.latest_chapter != null) {
                chapterList.add(comic.latest_chapter.chapter_title);
            }

            // TODO: Thay thế mã này để tải danh sách chương từ API
            // Bạn có thể thêm chương giả để test
            for (int i = 1; i <= 10; i++) {
                chapterList.add("Chương " + i);
            }

            // Tạo adapter và thiết lập RecyclerView
            ChapterAdapter adapter = new ChapterAdapter(this, chapterList);
            chapterRecyclerView.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up chapters list", e);
        }
    }

    @Override
    protected void onDestroy() {
        // Dọn dẹp tài nguyên, đặc biệt là Glide
        try {
            if (detailThumb != null) {
                Glide.with(this).clear(detailThumb);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up resources", e);
        }
        super.onDestroy();
    }
}
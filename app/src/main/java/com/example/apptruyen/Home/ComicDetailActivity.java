package com.example.apptruyen.Home;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.apptruyen.R;
import com.example.apptruyen.model.Comic;

public class ComicDetailActivity extends AppCompatActivity {

    private ImageView detailThumb;
    private TextView detailName, detailStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic_detail); // Tạo file XML này nữa nhé

        detailThumb = findViewById(R.id.detailThumb);
        detailName = findViewById(R.id.detailName);
        detailStatus = findViewById(R.id.detailStatus);
        Button readButton = findViewById(R.id.readButton);
        readButton.setOnClickListener(v -> {
            // Xử lý khi người dùng nhấn nút "Đọc truyện"
        });

        Comic comic = (Comic) getIntent().getSerializableExtra("comic");
        if (comic != null) {
            detailName.setText(comic.name);
            detailStatus.setText(comic.status);

            String imageUrl = "https://img.otruyenapi.com/uploads/comics/" + comic.thumb_url;
            Glide.with(this)
                    .load(imageUrl)
//                    .placeholder(R.drawable.placeholder_image) // Thêm hình placeholder
//                    .error(R.drawable.error_image) // Thêm hình khi lỗi
                    .into(detailThumb);

            // Thêm xử lý cho nút đọc truyện
            readButton.setOnClickListener(v -> {
                // Thêm code mở Activity đọc truyện ở đây
                // Intent readIntent = new Intent(this, ReadComicActivity.class);
                // readIntent.putExtra("comic_id", comic.id);
                // startActivity(readIntent);
                Toast.makeText(this, "Chức năng đọc truyện đang được phát triển", Toast.LENGTH_SHORT).show();
            });
        } else {
            // Xử lý khi không có dữ liệu truyện
            Toast.makeText(this, "Không thể tải thông tin truyện", Toast.LENGTH_SHORT).show();
            finish(); // Đóng activity nếu không có dữ liệu
        }
    }
}

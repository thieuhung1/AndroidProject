package com.example.apptruyen.Home;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.apptruyen.R;
import com.example.apptruyen.firebase.ImageAdapter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ChapterContentActivity extends AppCompatActivity {
    private static final String TAG = "ChapterContentActivity";
    private ProgressBar progressBar;
    private RecyclerView recyclerImages;
    private TextView tvNoContent;
    private Toolbar toolbar;
    private ImageAdapter imageAdapter;
    private List<String> imageUrls = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_content);

        // Khởi tạo các thành phần giao diện
        progressBar = findViewById(R.id.progressBar);
        recyclerImages = findViewById(R.id.recyclerImages);
        tvNoContent = findViewById(R.id.tvNoContent);
        toolbar = findViewById(R.id.toolbar);

        // Thiết lập Toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Khởi tạo RecyclerView
        recyclerImages.setLayoutManager(new LinearLayoutManager(this));
        imageAdapter = new ImageAdapter(imageUrls);
        recyclerImages.setAdapter(imageAdapter);

        // Lấy dữ liệu từ Intent
        String chapterId = getIntent().getStringExtra("chapter_id");
        String chapterName = getIntent().getStringExtra("chapter_name");
        String chapterTitle = getIntent().getStringExtra("chapter_title");

        // Kiểm tra ID chương
        if (chapterId == null) {
            Log.e(TAG, "ID chương không hợp lệ");
            Toast.makeText(this, "ID chương không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Cập nhật tiêu đề Toolbar
        String title = chapterTitle != null && !chapterTitle.isEmpty() ? chapterTitle : "Chương " + chapterName;
        getSupportActionBar().setTitle(title);

        // Tái tạo URL chương
        String chapterApiUrl = "https://sv1.otruyencdn.com/v1/api/chapter/" + chapterId;
        Log.d(TAG, "Đang tải chương ID: " + chapterId + ", URL: " + chapterApiUrl);

        // Tải nội dung chương
        loadChapterContent(chapterApiUrl);
    }

    private void loadChapterContent(String chapterApiUrl) {
        progressBar.setVisibility(View.VISIBLE);
        recyclerImages.setVisibility(View.GONE);
        tvNoContent.setVisibility(View.GONE);

        new Thread(() -> {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL(chapterApiUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Mã phản hồi API: " + responseCode);

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        tvNoContent.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Lỗi API: HTTP " + responseCode, Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                String jsonResponse = result.toString();
                Log.d(TAG, "Phản hồi API: " + jsonResponse);

                JSONObject json = new JSONObject(jsonResponse);
                String status = json.optString("status", "");
                if (!status.equalsIgnoreCase("success")) {
                    String message = json.optString("message", "Lỗi không xác định");
                    Log.e(TAG, "API trả về lỗi: " + message);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        tvNoContent.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Lỗi API: " + message, Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                JSONObject data = json.getJSONObject("data");
                JSONObject item = data.getJSONObject("item");
                JSONArray images = item.getJSONArray("chapter_image");

                List<String> newImageUrls = new ArrayList<>();
                for (int i = 0; i < images.length(); i++) {
                    JSONObject image = images.getJSONObject(i);
                    String imageUrl = image.optString("image_cdn", "");
                    if (!imageUrl.isEmpty()) {
                        newImageUrls.add(imageUrl);
                    }
                }

                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    progressBar.setVisibility(View.GONE);
                    if (newImageUrls.isEmpty()) {
                        tvNoContent.setVisibility(View.VISIBLE);
                    } else {
                        imageUrls.clear();
                        imageUrls.addAll(newImageUrls);
                        imageAdapter.notifyDataSetChanged();
                        recyclerImages.setVisibility(View.VISIBLE);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi tải nội dung chương: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    if (!isFinishing()) {
                        progressBar.setVisibility(View.GONE);
                        tvNoContent.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi khi đóng reader", e);
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }
}
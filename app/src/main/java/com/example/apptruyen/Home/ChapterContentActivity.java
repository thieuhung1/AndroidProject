package com.example.apptruyen.Home;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
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
import com.example.apptruyen.api.ApiClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChapterContentActivity extends AppCompatActivity {
    private static final String TAG = "ChapterContentActivity";

    private Toolbar toolbar;
    private TextView tvNoContent;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;

    private String chapterUrl;
    private String chapterName;
    private String chapterTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_content);

        initViews();
        getIntentData();
        fetchChapterContent();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvNoContent = findViewById(R.id.tvNoContent);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerImages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        imageAdapter = new ImageAdapter(new ArrayList<>());
        recyclerView.setAdapter(imageAdapter);
    }

    private void getIntentData() {
        chapterUrl = getIntent().getStringExtra("chapter_api_data");
        chapterName = getIntent().getStringExtra("chapter_name");
        chapterTitle = getIntent().getStringExtra("chapter_title");

        // Set tiêu đề cho toolbar
        String title = "Chapter " + chapterName;
        if (chapterTitle != null && !chapterTitle.isEmpty()) {
            title += ": " + chapterTitle;
        }
        setTitle(title);

        if (chapterUrl == null || chapterUrl.isEmpty()) {
            showError("URL chapter không hợp lệ");
        }
    }

    private void fetchChapterContent() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoContent.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        // Gọi API bằng OkHttp
        ApiClient.get(chapterUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API call failed: " + e.getMessage());
                runOnUiThread(() -> showError("Lỗi kết nối: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> showError("Lỗi máy chủ: " + response.code()));
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);

                    // Kiểm tra status API
                    if (!json.getString("status").equals("success")) {
                        runOnUiThread(() -> showError("Lỗi dữ liệu từ API"));
                        return;
                    }

                    // Parse dữ liệu ảnh
                    JSONObject data = json.getJSONObject("data");
                    JSONArray images = data.getJSONArray("images");
                    String domain = data.getString("domain_image");

                    List<String> imageUrls = new ArrayList<>();
                    for (int i = 0; i < images.length(); i++) {
                        imageUrls.add(domain + images.getString(i));
                    }

                    // Cập nhật UI trên luồng chính
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (imageUrls.isEmpty()) {
                            tvNoContent.setText("Không có nội dung");
                            tvNoContent.setVisibility(View.VISIBLE);
                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                            imageAdapter.updateImages(imageUrls);
                        }
                    });

                } catch (JSONException e) {
                    Log.e(TAG, "JSON parsing error: " + e.getMessage());
                    runOnUiThread(() -> showError("Lỗi xử lý dữ liệu"));
                }
            }
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            tvNoContent.setText(message);
            tvNoContent.setVisibility(View.VISIBLE);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
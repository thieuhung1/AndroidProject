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
import com.example.apptruyen.R;
import com.example.apptruyen.model.Comic;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.apptruyen.firebase.ChapterAdapter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONObject;

public class ChapterDetail extends AppCompatActivity {
    private static final String TAG = "ChapterDetail";
    private List<Comic.Chapter> chapterList = new ArrayList<>();
    private ImageView imgBiaTruyen;
    private TextView tvTenTruyen, tvTacGia, tvTheLoai, tvGioiThieu;
    private Button btnDocTruyen;
    private RecyclerView recyclerChapters;
    private ChapterAdapter chapterAdapter;
    private FirebaseFirestore db;
    private String comicId,slug;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chapterdetail);
        db = FirebaseFirestore.getInstance();
        initViews();
        recyclerChapters.setLayoutManager(new LinearLayoutManager(this));
        chapterAdapter = new ChapterAdapter(new ArrayList<>());
        recyclerChapters.setAdapter(chapterAdapter);
        comicId = getIntent().getStringExtra("comicId");

        if (comicId == null) {
            showErrorAndFinish("Không có ID truyện");
            return;
        }
        loadComicData();

    }

    private void initViews() {
        imgBiaTruyen = findViewById(R.id.imgBiaTruyen);
        tvTenTruyen = findViewById(R.id.tvTenTruyen);
        tvTacGia = findViewById(R.id.tvTacGia);
        tvTheLoai = findViewById(R.id.tvTheLoai);
        tvGioiThieu = findViewById(R.id.tvGioiThieu);
        btnDocTruyen = findViewById(R.id.btnDocTruyen);
        recyclerChapters = findViewById(R.id.recyclerChapters);
        slug = getIntent().getStringExtra("slug");

        Log.d(TAG, "Slug truyện: " + slug);
        if (slug == null || slug.isEmpty()) {
            Log.e(TAG, "Slug truyện bị null hoặc rỗng, không gọi API được");
            Toast.makeText(this, "Slug truyện không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        loadChaptersFromApi(slug);

        btnDocTruyen.setOnClickListener(v -> {
            if (chapterList.size() > 0) {
                // Lấy chapter đầu tiên (thường là chapter có số index lớn nhất trong danh sách)
                Comic.Chapter firstChapter = chapterList.get(chapterList.size() - 1);
                Intent intent = new Intent(this, ChapterContentActivity.class);
                intent.putExtra("chapter_api_url", firstChapter.chapter_api_data);
                intent.putExtra("chapter_name", firstChapter.chapter_name);
                intent.putExtra("chapter_title", firstChapter.chapter_title);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Không có chapter nào để đọc", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadComicData() {
        db.collection("comics").document(comicId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            updateUI(document);

                        } else {
                            showErrorAndFinish("Truyện không tồn tại");
                        }
                    } else {
                        Log.e(TAG, "Firestore error: ", task.getException());
                        showErrorAndFinish("Lỗi khi tải dữ liệu");
                    }
                });
    }

    private void updateUI(DocumentSnapshot document) {
        try {
            tvTenTruyen.setText(document.getString("slug"));
            tvTacGia.setText("Tác giả: " + document.getString("author"));
            tvGioiThieu.setText(document.getString("description"));
            List<String> categories = (List<String>) document.get("category");
            if (categories != null && !categories.isEmpty()) {
                tvTheLoai.setText("Thể loại: " + String.join(", ", categories));
            }
            String imageUrl = document.getString("thumb_url");
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                        .load("https://img.otruyenapi.com/uploads/comics/" + imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.placeholder_image)
                        .into(imgBiaTruyen);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing document", e);
            showErrorAndFinish("Lỗi hiển thị dữ liệu");
        }
    }

//    private void loadChapters(DocumentSnapshot document) {
//        List<Comic.Chapter> chapters = new ArrayList<>();
//
//        try {
//
//            @SuppressWarnings("unchecked")
//            List<Map<String, Object>> chaptersLatest = (List<Map<String, Object>>) document.get("chaptersLatest");
//
//            if (chaptersLatest != null && !chaptersLatest.isEmpty()) {
//                for (Map<String, Object> chapterMap : chaptersLatest) {
//                    Comic.Chapter chapter = new Comic.Chapter();
//                    chapter.filename = (String) chapterMap.get("filename");
//                    chapter.chapter_name = (String) chapterMap.get("chapter_name");
//                    chapter.chapter_title = (String) chapterMap.get("chapter_title");
//                    chapter.chapter_api_data = (String) chapterMap.get("chapter_api_data");
//                    chapters.add(chapter);
//                }
//
//
//                Collections.sort(chapters, (c1, c2) -> {
//                    try {
//                        int num1 = Integer.parseInt(c1.chapter_name.replaceAll("\\D+", ""));
//                        int num2 = Integer.parseInt(c2.chapter_name.replaceAll("\\D+", ""));
//                        return num2 - num1;
//                    } catch (NumberFormatException e) {
//                        return c2.chapter_name.compareTo(c1.chapter_name);
//                    }
//                });
//
//                this.chapterList = chapters;
//                chapterAdapter.updateChapters(chapters);
//            } else {
//                Toast.makeText(this, "Không có chapter nào", Toast.LENGTH_SHORT).show();
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Error parsing chapters", e);
//            Toast.makeText(this, "Lỗi khi tải danh sách chapter", Toast.LENGTH_SHORT).show();
//        }
//    }

    private void loadChaptersFromApi(String slug) {
        new Thread(() -> {
            try {
                URL url = new URL("https://otruyenapi.com/v1/api/truyen-tranh/" + slug);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000); // Add timeout

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "API Response Code: " + responseCode);

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> Toast.makeText(this,
                            "API Error: " + responseCode, Toast.LENGTH_SHORT).show());
                    return;
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                String jsonResponse = result.toString();
                Log.d(TAG, "API Response (first 200 chars): " +
                        jsonResponse.substring(0, Math.min(jsonResponse.length(), 200)));

                JSONObject json = new JSONObject(jsonResponse);

                // Verify API returned success status
                boolean status = json.optBoolean("status", false);
                if (!status) {
                    String message = json.optString("message", "Unknown error");
                    Log.e(TAG, "API returned error: " + message);
                    runOnUiThread(() -> Toast.makeText(this,
                            "API Error: " + message, Toast.LENGTH_SHORT).show());
                    return;
                }

                JSONArray chapterArray = json.getJSONObject("data").getJSONArray("chapters");
                Log.d(TAG, "Found " + chapterArray.length() + " chapters in API response");

                List<Comic.Chapter> chapters = new ArrayList<>();
                for (int i = 0; i < chapterArray.length(); i++) {
                    JSONObject chap = chapterArray.getJSONObject(i);
                    Comic.Chapter chapter = new Comic.Chapter();

                    chapter.chapter_name = chap.optString("chapter_name", "Chapter ?");
                    chapter.chapter_title = chap.optString("chapter_title", "");
                    chapter.chapter_api_data = chap.optString("chapter_api_data", "");
                    chapter.filename = chap.optString("filename", "");

                    chapters.add(chapter);
                }

                final List<Comic.Chapter> finalChapters = chapters;
                runOnUiThread(() -> {
                    if (finalChapters.isEmpty()) {
                        Toast.makeText(this, "No chapters found", Toast.LENGTH_SHORT).show();
                    } else {
                        this.chapterList = finalChapters;
                        chapterAdapter.updateChapters(finalChapters);
                        // Force adapter to refresh
                        chapterAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Updated adapter with " + finalChapters.size() + " chapters");
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading chapters from API", e);
                runOnUiThread(() -> Toast.makeText(this,
                        "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }
}
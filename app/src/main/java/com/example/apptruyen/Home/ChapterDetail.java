package com.example.apptruyen.Home;

import android.content.Intent;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ChapterDetail extends AppCompatActivity {
    private static final String TAG = "ChapterDetail";
    private List<Comic.Chapter> chapterList = new ArrayList<>();
    private ImageView imgBiaTruyen;
    private TextView tvTenTruyen, tvTacGia, tvTheLoai, tvGioiThieu;
    private Button btnDocTruyen;
    private RecyclerView recyclerChapters;
    private ChapterAdapter chapterAdapter;
    private FirebaseFirestore db;
    private String comicId, slug, comicslug;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chapterdetail);
        db = FirebaseFirestore.getInstance();
        initViews();

        // Initialize RecyclerView and adapter
        chapterAdapter = new ChapterAdapter(new ArrayList<>());
        recyclerChapters.setAdapter(chapterAdapter);
        recyclerChapters.setLayoutManager(new LinearLayoutManager(this));

        comicId = getIntent().getStringExtra("comicId");
        comicslug = getIntent().getStringExtra("slug");

        loadComicData();
        loadChaptersFromApi(comicslug);
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



        btnDocTruyen.setOnClickListener(v -> {
            if (!chapterList.isEmpty()) {
                // Sort chapters by chapter_name (descending) and pick the latest
                Collections.sort(chapterList, (c1, c2) -> {
                    try {
                        int num1 = Integer.parseInt(c1.chapter_name.replaceAll("\\D+", ""));
                        int num2 = Integer.parseInt(c2.chapter_name.replaceAll("\\D+", ""));
                        return num2 - num1; // Descending order
                    } catch (NumberFormatException e) {
                        return c2.chapter_name.compareTo(c1.chapter_name);
                    }
                });
                Comic.Chapter latestChapter = chapterList.get(0); // Latest chapter after sorting
                Intent intent = new Intent(this, ChapterContentActivity.class);
                intent.putExtra("chapter_api_url", latestChapter.chapter_api_data);
                intent.putExtra("chapter_name", latestChapter.chapter_name);
                intent.putExtra("chapter_title", latestChapter.chapter_title);
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

    private void loadChaptersFromApi(String slug) {
        if (slug == null || slug.isEmpty()) {
            runOnUiThread(() -> Toast.makeText(this, "Invalid comic slug", Toast.LENGTH_SHORT).show());
            return;
        }

        new Thread(() -> {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL("https://otruyenapi.com/v1/api/truyen-tranh/" + slug);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "API Response Code: " + responseCode);

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> Toast.makeText(this,
                            "API Error: HTTP " + responseCode, Toast.LENGTH_SHORT).show());
                    return;
                }

                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                String jsonResponse = result.toString();
                Log.d(TAG, "Full API Response: " + jsonResponse);

                JSONObject json = new JSONObject(jsonResponse);
                String status = json.optString("status", "");
                if (!status.equalsIgnoreCase("success")) {
                    String message = json.optString("message", "Unknown error");
                    Log.e(TAG, "API returned error: " + message);
                    runOnUiThread(() -> Toast.makeText(this,
                            "API Error: " + message, Toast.LENGTH_SHORT).show());
                    return;
                }

                JSONObject data = json.getJSONObject("data");
                JSONObject item = data.getJSONObject("item");
                JSONArray chaptersArray = item.getJSONArray("chapters");
                if (chaptersArray.length() == 0) {
                    runOnUiThread(() -> Toast.makeText(this, "No chapters found", Toast.LENGTH_SHORT).show());
                    return;
                }

                JSONArray chapterServerData = chaptersArray.getJSONObject(0).getJSONArray("server_data");
                Log.d(TAG, "Found " + chapterServerData.length() + " chapters in API response");

                List<Comic.Chapter> chapters = IntStream.range(0, chapterServerData.length())
                        .mapToObj(i -> {
                            try {
                                JSONObject chap = chapterServerData.getJSONObject(i);
                                Comic.Chapter chapter = new Comic.Chapter();
                                chapter.chapter_name = chap.optString("chapter_name", "Unknown Chapter");
                                chapter.chapter_title = chap.optString("chapter_title", "");
                                chapter.chapter_api_data = chap.optString("chapter_api_data", "");
                                chapter.filename = chap.optString("filename", "");
                                Log.d(TAG, "Chapter " + i + ": name=" + chapter.chapter_name);
                                return chapter;
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing chapter at index " + i, e);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (chapters.isEmpty()) {
                        Toast.makeText(this, "No chapters found", Toast.LENGTH_SHORT).show();
                    } else {
                        chapterList = chapters;
                        chapterAdapter.updateChapters(chapters);
                        chapterAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Updated adapter with " + chapters.size() + " chapters");
                    }
                });

            } catch (MalformedURLException e) {
                Log.e(TAG, "Invalid URL: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, "Error: Invalid URL", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "Network error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, "Error: Network issue", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (JSONException e) {
                Log.e(TAG, "JSON parsing error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, "Error: Invalid response format", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing reader", e);
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }
}
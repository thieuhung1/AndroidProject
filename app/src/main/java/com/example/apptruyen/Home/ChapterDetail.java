package com.example.apptruyen.Home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.apptruyen.R;
import com.example.apptruyen.firebase.ChapterAdapter;
import com.example.apptruyen.model.Comic;
import com.google.firebase.firestore.FirebaseFirestore;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChapterDetail extends AppCompatActivity {
    private static final String API_URL = "https://otruyenapi.com/v1/api/truyen-tranh/";
    private static final String PREFS_NAME = "FavoriteComics";
    private static final String FAVORITE_KEY = "favorite_comics";

    private ImageView imgBiaTruyen, btnYeuThich, btnBack;
    private TextView tvTenTruyen, tvTacGia, tvTheLoai, tvGioiThieu;
    private Button btnDocTruyen;
    private RecyclerView recyclerChapters;
    private ChapterAdapter chapterAdapter;
    private final List<Comic.Chapter> chapterList = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private SharedPreferences prefs;
    private FirebaseFirestore db;
    private String comicId, comicslug;
    private boolean isFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chapterdetail);

        imgBiaTruyen = findViewById(R.id.imgBiaTruyen);
        tvTenTruyen = findViewById(R.id.tvTenTruyen);
        tvTacGia = findViewById(R.id.tvTacGia);
        tvTheLoai = findViewById(R.id.tvTheLoai);
        tvGioiThieu = findViewById(R.id.tvGioiThieu);
        btnDocTruyen = findViewById(R.id.btnDocTruyen);
        btnYeuThich = findViewById(R.id.btnYeuThich);
        btnBack = findViewById(R.id.btnBack);
        recyclerChapters = findViewById(R.id.recyclerChapters);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();

        comicId = getIntent().getStringExtra("comicId");
        comicslug = getIntent().getStringExtra("slug");

        chapterAdapter = new ChapterAdapter(chapterList);
        recyclerChapters.setLayoutManager(new LinearLayoutManager(this));
        recyclerChapters.setAdapter(chapterAdapter);

        btnBack.setOnClickListener(v -> finish());
        btnDocTruyen.setOnClickListener(v -> startFirstChapter());
        btnYeuThich.setOnClickListener(v -> toggleFavorite());

        checkFavoriteStatus();
        loadComicData();
        if (comicslug != null && !comicslug.isEmpty()) loadChapters(comicslug);
        else toast("Slug truyện không hợp lệ");
    }

    private void checkFavoriteStatus() {
        isFavorite = prefs.getStringSet(FAVORITE_KEY, Collections.emptySet()).contains(comicId);
        btnYeuThich.setImageResource(isFavorite ? R.drawable.ic_like_filled : R.drawable.ic_like);
    }

    private void toggleFavorite() {
        Set<String> favorites = new HashSet<>(prefs.getStringSet(FAVORITE_KEY, new HashSet<>()));
        if (isFavorite) favorites.remove(comicId);
        else favorites.add(comicId);
        isFavorite = !isFavorite;
        prefs.edit().putStringSet(FAVORITE_KEY, favorites).apply();
        btnYeuThich.setImageResource(isFavorite ? R.drawable.ic_like_filled : R.drawable.ic_like);
        toast(isFavorite ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích");
    }

    private void loadComicData() {
        db.collection("comics").document(comicId).get()
                .addOnSuccessListener(doc -> {
                    tvTenTruyen.setText(doc.getString("name"));
                    tvTacGia.setText("Tác giả: " + doc.getString("author"));
                    tvGioiThieu.setText(doc.getString("description"));
                    List<String> cats = (List<String>) doc.get("category");
                    if (cats != null) tvTheLoai.setText("Thể loại: " + String.join(", ", cats));

                    String thumb = doc.getString("thumb_url");
                    if (thumb != null && !thumb.isEmpty())
                        Glide.with(this)
                                .load("https://img.otruyenapi.com/uploads/comics/" + thumb)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .placeholder(R.drawable.placeholder_image)
                                .into(imgBiaTruyen);
                })
                .addOnFailureListener(e -> finishWithError("Lỗi tải dữ liệu truyện"));
    }

    private void loadChapters(String slug) {
        executor.execute(() -> {
            try {
                String cacheKey = "cached_chapter_" + slug;
                String jsonStr = prefs.getString(cacheKey, null);
                if (jsonStr == null) {
                    OkHttpClient client = new OkHttpClient();
                    Response response = client.newCall(new Request.Builder().url(API_URL + slug).build()).execute();
                    if (!response.isSuccessful()) {
                        toastUi("Lỗi API: " + response.code());
                        return;
                    }
                    jsonStr = response.body().string();
                    prefs.edit().putString(cacheKey, jsonStr).apply();
                }

                JSONArray serverData = new JSONObject(jsonStr)
                        .getJSONObject("data").getJSONObject("item")
                        .getJSONArray("chapters").getJSONObject(0)
                        .getJSONArray("server_data");

                List<Comic.Chapter> chapters = new ArrayList<>();
                for (int i = 0; i < serverData.length(); i++) {
                    JSONObject obj = serverData.getJSONObject(i);
                    Comic.Chapter c = new Comic.Chapter();
                    c.chapter_name = obj.optString("chapter_name");
                    c.chapter_title = obj.optString("chapter_title");
                    c.chapter_api_data = obj.optString("chapter_api_data");
                    c.filename = obj.optString("filename");
                    chapters.add(c);
                }

                runOnUiThread(() -> {
                    chapterList.clear();
                    chapterList.addAll(chapters);
                    chapterAdapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                Log.e("ChapterDetail", "Lỗi tải chương", e);
                toastUi(e instanceof java.io.IOException ? "Lỗi mạng" : "Lỗi xử lý dữ liệu");
            }
        });
    }

    private void startFirstChapter() {
        if (chapterList.isEmpty()) {
            toast("Không có chương nào để đọc");
            return;
        }
        Comic.Chapter first = Collections.min(chapterList, Comparator.comparingInt(c -> extractNumber(c.chapter_name)));
        String chapterId = extractChapterId(first.chapter_api_data);
        if (chapterId == null) {
            toast("Lỗi dữ liệu chương");
            return;
        }

        Intent intent = new Intent(this, ChapterContentActivity.class);
        intent.putExtra("chapter_id", chapterId);
        intent.putExtra("chapter_name", first.chapter_name);
        intent.putExtra("chapter_title", first.chapter_title);
        startActivity(intent);
    }

    private int extractNumber(String text) {
        try { return Integer.parseInt(text.replaceAll("\\D+", "")); }
        catch (Exception e) { return 0; }
    }

    private String extractChapterId(String apiData) {
        try {
            String[] parts = apiData.split("/");
            return parts[parts.length - 1];
        } catch (Exception e) {
            return null;
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void toastUi(String msg) {
        runOnUiThread(() -> toast(msg));
    }

    private void finishWithError(String msg) {
        toast(msg);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}

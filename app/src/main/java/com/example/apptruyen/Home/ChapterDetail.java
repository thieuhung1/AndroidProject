package com.example.apptruyen.Home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
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
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChapterDetail extends AppCompatActivity {
    private static final String API_URL = "https://otruyenapi.com/v1/api/truyen-tranh/";
    private static final String FAVORITE_KEY = "favorite_comics";
    private static final long CACHE_EXPIRY = 24 * 60 * 60 * 1000; // 1 ngày

    private ImageView imgBiaTruyen, btnYeuThich, btnBack;
    private TextView tvTenTruyen, tvTacGia, tvTheLoai, tvGioiThieu;
    private Button btnDocTruyen;
    private RecyclerView recyclerChapters;
    private FrameLayout progressOverlay;
    private ProgressBar progressBar;
    private ChapterAdapter chapterAdapter;
    private final List<Comic.Chapter> chapterList = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private SharedPreferences prefs, cachePrefs;
    private FirebaseFirestore db;
    private String comicId, comicslug, username;
    private boolean isFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chapterdetail);

        initViews();
        initPrefs();
        setupRecyclerView();
        loadData();
    }

    private void initViews() {
        imgBiaTruyen = findViewById(R.id.imgBiaTruyen);
        tvTenTruyen = findViewById(R.id.tvTenTruyen);
        tvTacGia = findViewById(R.id.tvTacGia);
        tvTheLoai = findViewById(R.id.tvTheLoai);
        tvGioiThieu = findViewById(R.id.tvGioiThieu);
        btnDocTruyen = findViewById(R.id.btnDocTruyen);
        btnYeuThich = findViewById(R.id.btnYeuThich);
        btnBack = findViewById(R.id.btnBack);
        recyclerChapters = findViewById(R.id.recyclerChapters);

        progressOverlay = new FrameLayout(this);
        progressOverlay.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        progressOverlay.setBackgroundColor(0x80000000);
        progressBar = new ProgressBar(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = android.view.Gravity.CENTER;
        progressBar.setLayoutParams(params);
        progressOverlay.addView(progressBar);
        ((FrameLayout) findViewById(android.R.id.content)).addView(progressOverlay);
        progressOverlay.setVisibility(View.GONE);

        btnBack.setOnClickListener(v -> finishWithTransition());
        btnDocTruyen.setOnClickListener(v -> startFirstChapter());
        btnYeuThich.setOnClickListener(v -> toggleFavorite());
    }

    private void initPrefs() {
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        username = userPrefs.getString("username", null);
        if (username == null) {
            toast("Không xác định tài khoản!");
            finish();
            return;
        }
        prefs = getSharedPreferences("FavoriteComics_" + username, MODE_PRIVATE);
        cachePrefs = getSharedPreferences("chapter_cache", MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();
        comicId = getIntent().getStringExtra("comicId");
        comicslug = getIntent().getStringExtra("slug");
    }

    private void setupRecyclerView() {
        recyclerChapters.setLayoutManager(new LinearLayoutManager(this));
        recyclerChapters.setHasFixedSize(true);
        chapterAdapter = new ChapterAdapter(chapterList);
        recyclerChapters.setAdapter(chapterAdapter);
    }

    private void loadData() {
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
        isFavorite = !isFavorite;
        if (isFavorite) favorites.add(comicId);
        else favorites.remove(comicId);
        prefs.edit().putStringSet(FAVORITE_KEY, favorites).apply();
        btnYeuThich.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        btnYeuThich.setImageResource(isFavorite ? R.drawable.ic_like_filled : R.drawable.ic_like);
        toast(isFavorite ? "Đã thêm yêu thích" : "Đã xóa yêu thích");
    }

    private void loadComicData() {
        showProgress(true);
        db.collection("comics").document(comicId).get()
                .addOnSuccessListener(doc -> {
                    showProgress(false);
                    if (doc.exists()) {
                        tvTenTruyen.setText(doc.getString("name"));
                        tvTacGia.setText("Tác giả: " + doc.getString("author"));
                        tvGioiThieu.setText(doc.getString("description"));
                        List<String> cats = (List<String>) doc.get("category");
                        if (cats != null) tvTheLoai.setText("Thể loại: " + String.join(", ", cats));

                        String thumb = doc.getString("thumb_url");
                        if (thumb != null && !thumb.isEmpty()) {
                            Glide.with(this)
                                    .load("https://img.otruyenapi.com/uploads/comics/" + thumb)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .placeholder(R.drawable.placeholder_image)
                                    .thumbnail(0.25f)
                                    .into(imgBiaTruyen);
                            imgBiaTruyen.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
                        }
                    } else {
                        finishWithError("Không tìm thấy truyện");
                    }
                })
                .addOnFailureListener(e -> finishWithError("Lỗi tải truyện"));
    }

    private void loadChapters(String slug) {
        showProgress(true);
        String cacheKey = "chapters_" + slug;
        long lastCached = cachePrefs.getLong("cache_time_" + slug, 0);
        if (System.currentTimeMillis() - lastCached < CACHE_EXPIRY) {
            String cachedChapters = cachePrefs.getString(cacheKey, null);
            if (cachedChapters != null) {
                try {
                    List<Comic.Chapter> chapters = new Gson().fromJson(cachedChapters, new com.google.gson.reflect.TypeToken<List<Comic.Chapter>>(){}.getType());
                    updateChapters(chapters);
                    return;
                } catch (Exception e) {
                    toastUi("Lỗi đọc cache");
                }
            }
        }

        executor.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Response response = client.newCall(new Request.Builder().url(API_URL + slug).build()).execute();
                if (!response.isSuccessful()) {
                    toastUi("Lỗi API: " + response.code());
                    showProgress(false);
                    return;
                }

                JSONArray serverData = new JSONObject(response.body().string())
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

                cachePrefs.edit()
                        .putString(cacheKey, new Gson().toJson(chapters))
                        .putLong("cache_time_" + slug, System.currentTimeMillis())
                        .apply();

                runOnUiThread(() -> updateChapters(chapters));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    toast(e instanceof java.io.IOException ? "Lỗi mạng" : "Lỗi tải");
                });
            }
        });
    }

    private void updateChapters(List<Comic.Chapter> chapters) {
        chapterList.clear();
        chapterList.addAll(chapters);
        chapterAdapter.updateChapters(chapters);
        recyclerChapters.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        showProgress(false);
    }

    private void startFirstChapter() {
        if (chapterList.isEmpty()) {
            toast("Không có chương");
            return;
        }

        Comic.Chapter first = Collections.min(chapterList, Comparator.comparingInt(c -> extractNumber(c.chapter_name)));
        String chapterId = extractChapterId(first.chapter_api_data);
        if (chapterId == null) {
            toast("Lỗi dữ liệu");
            return;
        }

        markAsReadOnline(comicId);

        ArrayList<String> chapterIds = new ArrayList<>();
        for (Comic.Chapter c : chapterList) {
            String id = extractChapterId(c.chapter_api_data);
            if (id != null) chapterIds.add(id);
        }
        int currentIndex = chapterIds.indexOf(chapterId);

        Intent intent = new Intent(this, ChapterContentActivity.class);
        intent.putExtra("chapter_id", chapterId);
        intent.putExtra("chapter_name", first.chapter_name);
        intent.putExtra("chapter_title", first.chapter_title);
        intent.putStringArrayListExtra("chapter_ids", chapterIds);
        intent.putExtra("current_index", currentIndex);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void markAsReadOnline(String comicId) {
        if (username == null) return;
        db.collection("users").document(username).get()
                .addOnSuccessListener(doc -> {
                    List<String> read = (List<String>) doc.get("read_comics");
                    if (read == null) read = new ArrayList<>();
                    if (!read.contains(comicId)) {
                        read.add(comicId);
                        db.collection("users").document(username).update("read_comics", read);
                    }
                });
    }

    private int extractNumber(String text) {
        try { return Integer.parseInt(text.replaceAll("\\D+", "")); }
        catch (Exception e) { return 0; }
    }

    private String extractChapterId(String apiData) {
        try {
            if (apiData == null || apiData.isEmpty()) return null;
            String[] parts = apiData.split("/");
            return parts.length > 0 ? parts[parts.length - 1] : null;
        } catch (Exception e) { return null; }
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }

    private void toastUi(String msg) { runOnUiThread(() -> toast(msg)); }

    private void finishWithError(String msg) {
        toast(msg);
        finish();
    }

    private void finishWithTransition() {
        finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void showProgress(boolean show) {
        progressOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
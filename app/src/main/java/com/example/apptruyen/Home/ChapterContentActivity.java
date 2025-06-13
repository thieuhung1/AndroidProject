package com.example.apptruyen.Home;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.apptruyen.R;
import com.example.apptruyen.api.ApiService;
import com.example.apptruyen.api.ChapterResponse;
import com.example.apptruyen.firebase.ImageAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChapterContentActivity extends AppCompatActivity {
    private static final String BASE_URL = "https://sv1.otruyencdn.com/";
    private static final int TIMEOUT = 15;
    private static final long CLICK_DEBOUNCE_MS = 500;
    private static final String USER_PREFS = "user_prefs";

    private RecyclerView recyclerImages;
    private ProgressBar progressBar;
    private TextView tvToolbarTitle, tvNoContent;
    private ImageView btnBack;
    private View btnPrev, btnNext;
    private ImageAdapter imageAdapter;
    private final List<String> imageUrls = new ArrayList<>();
    private ApiService apiService;
    private ArrayList<String> chapterIds;
    private int currentIndex, scrollPosition = 0;
    private String chapterId, chapterName, chapterTitle, username;
    private long lastClickTime = 0;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_content);

        db = FirebaseFirestore.getInstance();
        username = getSharedPreferences(USER_PREFS, MODE_PRIVATE).getString("username", null);
        initViews();
        setupRecyclerView();
        setupRetrofit();
        getIntentData();
        loadChapter();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        recyclerImages = findViewById(R.id.recyclerImages);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        tvNoContent = findViewById(R.id.tvNoContent);
        btnPrev = findViewById(R.id.btnPrevChapter);
        btnNext = findViewById(R.id.btnNextChapter);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finishWithTransition());
        btnPrev.setOnClickListener(v -> navigateChapter(-1));
        btnNext.setOnClickListener(v -> navigateChapter(1));
        findViewById(R.id.retryButton).setOnClickListener(v -> loadChapter());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    private void setupRecyclerView() {
        recyclerImages.setLayoutManager(new LinearLayoutManager(this));
        recyclerImages.setHasFixedSize(true);
        imageAdapter = new ImageAdapter(imageUrls);
        recyclerImages.setAdapter(imageAdapter);
        recyclerImages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    saveReadingProgress();
                }
            }
        });
    }

    private void setupRetrofit() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        apiService = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);
    }

    private void getIntentData() {
        chapterIds = getIntent().getStringArrayListExtra("chapter_ids");
        currentIndex = getIntent().getIntExtra("current_index", -1);
        chapterId = getIntent().getStringExtra("chapter_id");
        chapterName = getIntent().getStringExtra("chapter_name");
        chapterTitle = getIntent().getStringExtra("chapter_title");

        if (chapterIds != null && currentIndex >= 0 && currentIndex < chapterIds.size()) {
            chapterId = chapterIds.get(currentIndex);
        }
        updateToolbarTitle();
        restoreReadingProgress();
    }

    private void updateToolbarTitle() {
        tvToolbarTitle.setText(chapterName != null ? chapterName + (chapterTitle != null && !chapterTitle.isEmpty() ? ": " + chapterTitle : "") : "");
    }

    private void loadChapter() {
        if (!isNetworkAvailable()) {
            showError("Không có mạng");
            return;
        }

        setUI(true, false, false);
        apiService.getChapterContent(chapterId).enqueue(new retrofit2.Callback<ChapterResponse>() {
            @Override
            public void onResponse(Call<ChapterResponse> call, retrofit2.Response<ChapterResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    showError("Không tải được dữ liệu");
                    return;
                }

                List<String> urls = Optional.ofNullable(response.body().data.item.chapterImage)
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(i -> response.body().data.domainCdn + "/" + response.body().data.item.chapterPath + "/" + i.imageFile)
                        .collect(Collectors.toList());

                if (urls.isEmpty()) {
                    showError("Không có ảnh");
                } else {
                    imageUrls.clear();
                    imageUrls.addAll(urls);
                    imageAdapter.notifyDataSetChanged();
                    recyclerImages.scrollToPosition(scrollPosition);
                    setUI(false, true, true);
                }
            }

            @Override
            public void onFailure(Call<ChapterResponse> call, Throwable t) {
                showError("Lỗi: " + t.getMessage());
            }
        });
    }

    private void navigateChapter(int direction) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime <= CLICK_DEBOUNCE_MS) return;
        lastClickTime = currentTime;

        if (chapterIds == null || chapterIds.isEmpty()) {
            toast("Danh sách chương trống");
            return;
        }

        int newIndex = currentIndex + direction;
        if (newIndex < 0 || newIndex >= chapterIds.size()) {
            toast(direction > 0 ? "Không có chương sau" : "Không có chương trước");
            return;
        }

        currentIndex = newIndex;
        chapterId = chapterIds.get(currentIndex);
        scrollPosition = 0; // Reset khi chuyển chapter
        loadChapter();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void saveReadingProgress() {
        if (username == null || chapterId == null) return;
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerImages.getLayoutManager();
        if (layoutManager != null) {
            scrollPosition = layoutManager.findFirstVisibleItemPosition();
            Map<String, Object> progress = new HashMap<>();
            progress.put("chapter_id", chapterId);
            progress.put("position", scrollPosition);
            db.collection("users").document(username)
                    .update("current_reading", progress);
        }
    }

    private void restoreReadingProgress() {
        if (username == null) return;
        db.collection("users").document(username).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Map<String, Object> reading = (Map<String, Object>) doc.get("current_reading");
                        if (reading != null && reading.containsKey("chapter_id") && reading.containsKey("position")) {
                            String savedChapterId = (String) reading.get("chapter_id");
                            scrollPosition = ((Number) reading.get("position")).intValue();
                            if (savedChapterId.equals(chapterId)) {
                                recyclerImages.scrollToPosition(scrollPosition);
                            }
                        }
                    }
                });
    }

    private void setUI(boolean loading, boolean showList, boolean showNav) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        recyclerImages.setVisibility(showList ? View.VISIBLE : View.GONE);
        findViewById(R.id.errorLayout).setVisibility(!loading && !showList ? View.VISIBLE : View.GONE);
        findViewById(R.id.navChapterContainer).setVisibility(showNav ? View.VISIBLE : View.GONE);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }

    private void showError(String msg) {
        toast(msg);
        setUI(false, false, false);
    }

    private void finishWithTransition() {
        saveReadingProgress();
        finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
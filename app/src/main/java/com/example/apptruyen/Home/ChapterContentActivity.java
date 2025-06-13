package com.example.apptruyen.Home;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
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

    private RecyclerView recyclerImages;
    private ProgressBar progressBar;
    private TextView tvToolbarTitle, tvNoContent;
    private ImageView btnBack;
    private View btnPrev, btnNext;
    private ImageAdapter imageAdapter;
    private final List<String> imageUrls = new ArrayList<>();
    private ApiService apiService;
    private ArrayList<String> chapterIds;
    private int currentIndex;
    private String chapterId, chapterName, chapterTitle;
    private long lastClickTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_content);

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
        loadChapter();
        recyclerImages.smoothScrollToPosition(0);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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
        finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
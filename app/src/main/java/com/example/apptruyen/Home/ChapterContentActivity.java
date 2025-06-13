package com.example.apptruyen.Home;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.apptruyen.R;
import com.example.apptruyen.api.ApiService;
import com.example.apptruyen.api.ChapterResponse;
import com.example.apptruyen.firebase.ImageAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChapterContentActivity extends AppCompatActivity {
    private static final String TAG = "ChapterContent";
    private static final String BASE_URL = "https://sv1.otruyencdn.com/";
    private static final int TIMEOUT = 15, MAX_RETRIES = 2;

    private ProgressBar progressBar;
    private RecyclerView recyclerImages;
    private TextView tvNoContent;
    private final List<String> imageUrls = new ArrayList<>();
    private ImageAdapter imageAdapter;
    private ApiService apiService;
    private String chapterId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_content);

        initViews();
        setupRecyclerView();
        setupRetrofit();

        // Get data from Intent
        chapterId = getIntent().getStringExtra("chapter_id");
        if (chapterId == null || chapterId.isEmpty()) {
            showError("ID chương không hợp lệ");
            finish();
            return;
        }

        loadChapter();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        recyclerImages = findViewById(R.id.recyclerImages);
        tvNoContent = findViewById(R.id.tvNoContent);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        findViewById(R.id.retryButton).setOnClickListener(v -> loadChapter());
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
                .cache(new okhttp3.Cache(new java.io.File(getCacheDir(), "http-cache"), 20 * 1024 * 1024))
                .retryOnConnectionFailure(true)
                .build();

        apiService = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo n = cm.getActiveNetworkInfo();
        return n != null && n.isConnected();
    }

    private void loadChapter() {
        if (!isNetworkAvailable()) {
            showError("Không có kết nối mạng");
            return;
        }
        setUI(true, false);
        fetchData(0);
    }

    private void fetchData(int attempt) {
        apiService.getChapterContent(chapterId).enqueue(new retrofit2.Callback<ChapterResponse>() {
            @Override
            public void onResponse(Call<ChapterResponse> call, retrofit2.Response<ChapterResponse> res) {
                if (!res.isSuccessful() || res.body() == null || !"success".equalsIgnoreCase(res.body().status)) {
                    retryOrError(attempt, "Lỗi tải: " + (res.code() != 0 ? res.code() : "Không có dữ liệu"));
                    return;
                }

                List<String> urls = Optional.ofNullable(res.body().data.item.chapterImage)
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(i -> res.body().data.domainCdn + "/" + res.body().data.item.chapterPath + "/" + i.imageFile)
                        .filter(u -> u != null && !u.isEmpty())
                        .collect(Collectors.toList());

                if (urls.isEmpty()) {
                    showError("Không tìm thấy ảnh");
                } else {
                    imageUrls.clear();
                    imageUrls.addAll(urls);
                    imageAdapter.notifyDataSetChanged();
                    recyclerImages.startAnimation(AnimationUtils.loadAnimation(ChapterContentActivity.this, R.anim.fade_in));
                    setUI(false, true);
                }
                setTitle(getIntent().getStringExtra("chapter_name") + ": " + getIntent().getStringExtra("chapter_title"));
            }

            @Override
            public void onFailure(Call<ChapterResponse> call, Throwable t) {
                retryOrError(attempt, t.getMessage());
            }
        });
    }

    private void retryOrError(int attempt, String msg) {
        if (attempt < MAX_RETRIES) fetchData(attempt + 1);
        else showError("Lỗi: " + msg);
    }

    private void setUI(boolean loading, boolean showImages) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        recyclerImages.setVisibility(showImages ? View.VISIBLE : View.GONE);
        findViewById(R.id.errorLayout).setVisibility(!loading && !showImages ? View.VISIBLE : View.GONE);
    }

    private void showError(String msg) {
        setUI(false, false);
        toast(msg);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
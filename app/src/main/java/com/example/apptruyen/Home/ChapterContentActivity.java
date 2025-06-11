package com.example.apptruyen.Home;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

/**
 * Activity hiển thị nội dung chương truyện với ảnh chất lượng cao.
 */
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
    private Call<ChapterResponse> currentCall; // Lưu call hiện tại để hủy

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_content);

        initViews();
        setupRecyclerView();
        setupRetrofit();

        // Lấy chapterId từ Intent
        chapterId = getIntent().getStringExtra("chapter_id");

        // Debug Intent
        Log.d(TAG, "Intent data: chapterId=" + chapterId);

        if (chapterId == null || chapterId.isEmpty()) {
            showError("ID chương không hợp lệ");
            finish();
            return;
        }
        loadChapter();
    }

    /**
     * Khởi tạo giao diện và sự kiện nút
     */
    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        recyclerImages = findViewById(R.id.recyclerImages);
        tvNoContent = findViewById(R.id.tvNoContent);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.retryButton).setOnClickListener(v -> loadChapter());

        // Tắt animation mặc định khi chuyển Activity
        overridePendingTransition(0, 0);
    }

    /**
     * Thiết lập RecyclerView để hiển thị ảnh
     */
    private void setupRecyclerView() {
        recyclerImages.setLayoutManager(new LinearLayoutManager(this));
        imageAdapter = new ImageAdapter(imageUrls);
        recyclerImages.setAdapter(imageAdapter);
    }

    /**
     * Thiết lập Retrofit với cache OkHttp
     */
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

    /**
     * Kiểm tra kết nối mạng
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo n = cm.getActiveNetworkInfo();
        return n != null && n.isConnected();
    }

    /**
     * Tải nội dung chương
     */
    private void loadChapter() {
        if (!isNetworkAvailable()) {
            showError("Không có kết nối mạng");
            return;
        }
        setUI(true, false);
        Log.d(TAG, "Loading chapter with chapterId: " + chapterId);
        fetchData(0);
    }

    /**
     * Gọi API lấy URL ảnh
     */
    private void fetchData(int attempt) {
        // Hủy call trước nếu còn tồn tại
        if (currentCall != null) {
            currentCall.cancel();
            Log.d(TAG, "Canceled previous API call");
        }

        currentCall = apiService.getChapterContent(chapterId);
        currentCall.enqueue(new retrofit2.Callback<ChapterResponse>() {
            @Override
            public void onResponse(Call<ChapterResponse> call, retrofit2.Response<ChapterResponse> res) {
                currentCall = null;
                if (!res.isSuccessful() || res.body() == null || !"success".equalsIgnoreCase(res.body().status)) {
                    Log.e(TAG, "API error: " + (res.code() != 0 ? res.code() : "No data"));
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
                    setUI(false, true);
                    Log.d(TAG, "Loaded " + urls.size() + " images for chapter");
                }
            }

            @Override
            public void onFailure(Call<ChapterResponse> call, Throwable t) {
                currentCall = null;
                if (!call.isCanceled()) {
                    Log.e(TAG, "API failure: " + t.getMessage());
                    retryOrError(attempt, t.getMessage());
                }
            }
        });
    }

    /**
     * Thử lại hoặc hiển thị lỗi
     */
    private void retryOrError(int attempt, String msg) {
        if (attempt < MAX_RETRIES) {
            Log.d(TAG, "Retrying API call, attempt: " + (attempt + 1));
            fetchData(attempt + 1);
        } else {
            showError("Lỗi: " + msg);
        }
    }

    /**
     * Cập nhật giao diện
     */
    private void setUI(boolean loading, boolean showImages) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        recyclerImages.setVisibility(showImages ? View.VISIBLE : View.GONE);
        findViewById(R.id.errorLayout).setVisibility(!loading && !showImages ? View.VISIBLE : View.GONE);
    }

    /**
     * Hiển thị thông báo lỗi
     */
    private void showError(String msg) {
        Log.e(TAG, msg);
        setUI(false, false);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy call khi Activity bị hủy
        if (currentCall != null) {
            currentCall.cancel();
            Log.d(TAG, "Canceled API call on destroy");
        }
        overridePendingTransition(0, 0);
    }
}
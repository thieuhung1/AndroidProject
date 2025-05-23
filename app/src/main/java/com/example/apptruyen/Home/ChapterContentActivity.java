package com.example.apptruyen.Home;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

public class ChapterContentActivity extends AppCompatActivity {
    private static final String TAG = "ChapterContentActivity";
    private ProgressBar progressBar;
    private RecyclerView recyclerImages;
    private TextView tvNoContent;
    private Toolbar toolbar;
    private ImageAdapter imageAdapter;
    private List<String> imageUrls = new ArrayList<>();
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_content);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        initializeRetrofit();

        String chapterId = getIntent().getStringExtra("chapter_id");
        String chapterName = getIntent().getStringExtra("chapter_name");
        String chapterTitle = getIntent().getStringExtra("chapter_title");

        if (chapterId == null || chapterId.isEmpty()) {
            showError("Invalid chapter ID");
            finish();
            return;
        }

        setToolbarTitle(chapterTitle, chapterName);
        loadChapterContent(chapterId);
    }

    private void initializeViews() {
        progressBar = findViewById(R.id.progressBar);
        recyclerImages = findViewById(R.id.recyclerImages);
        tvNoContent = findViewById(R.id.tvNoContent);
        toolbar = findViewById(R.id.toolbar);
        Button retryButton = findViewById(R.id.retryButton);
        retryButton.setOnClickListener(v -> loadChapterContent(getIntent().getStringExtra("chapter_id")));
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        recyclerImages.setHasFixedSize(true);
        recyclerImages.setLayoutManager(new LinearLayoutManager(this));
        imageAdapter = new ImageAdapter(imageUrls);
        recyclerImages.setAdapter(imageAdapter);
    }

    private void initializeRetrofit() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://sv1.otruyencdn.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);
    }

    private void setToolbarTitle(String chapterTitle, String chapterName) {
        String title = (chapterTitle != null && !chapterTitle.isEmpty()) ? chapterTitle : "Chapter " + chapterName;
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void loadChapterContent(String chapterId) {
        if (!isNetworkAvailable()) {
            showError("No network connection");
            return;
        }
        Log.d(TAG, "Loading chapter with ID: " + chapterId + ", Network: " +
                (isNetworkAvailable() ? "Connected" : "Disconnected"));
        setUIState(true, false);
        retryRequest(chapterId, 3, 0);
    }

    private void retryRequest(String chapterId, int maxRetries, int attempt) {
        if (attempt >= maxRetries) {
            showError("Failed to load chapter after " + maxRetries + " attempts");
            return;
        }
        apiService.getChapterContent(chapterId).enqueue(new Callback<ChapterResponse>() {
            @Override
            public void onResponse(Call<ChapterResponse> call, Response<ChapterResponse> response) {
                if (isFinishing()) return;
                handleResponse(response);
            }

            @Override
            public void onFailure(Call<ChapterResponse> call, Throwable t) {
                if (!isFinishing()) {
                    Log.e(TAG, "Attempt " + (attempt + 1) + " failed: " + t.getMessage());
                    if (t instanceof SocketTimeoutException) {
                        retryRequest(chapterId, maxRetries, attempt + 1);
                    } else {
                        showError("Error: " + t.getMessage());
                    }
                }
            }
        });
    }

    private void handleResponse(Response<ChapterResponse> response) {
        setUIState(false, false);
        if (!response.isSuccessful()) {
            showError("API error: HTTP " + response.code());
            return;
        }

        ChapterResponse chapterResponse = response.body();
        if (chapterResponse == null || !"success".equalsIgnoreCase(chapterResponse.status)) {
            showError(chapterResponse != null ? chapterResponse.message : "Unknown error");
            return;
        }

        List<String> newImageUrls = chapterResponse.data.item.chapterImage != null
                ? chapterResponse.data.item.chapterImage.stream()
                .filter(image -> image.imageFile != null && !image.imageFile.isEmpty())
                .map(image -> chapterResponse.data.domainCdn + "/" +
                        chapterResponse.data.item.chapterPath + "/" +
                        image.imageFile)
                .collect(Collectors.toList())
                : new ArrayList<>();

        if (newImageUrls.isEmpty()) {
            showError("No images found");
        } else {
            updateImages(newImageUrls);
        }
    }

    private void updateImages(List<String> newImageUrls) {
        imageUrls.clear();
        imageUrls.addAll(newImageUrls);
        imageAdapter.notifyDataSetChanged();
        setUIState(false, true);
    }

    private void setUIState(boolean isLoading, boolean hasContent) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerImages.setVisibility(hasContent ? View.VISIBLE : View.GONE);
        findViewById(R.id.errorLayout).setVisibility(!isLoading && !hasContent ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        Log.e(TAG, message);
        setUIState(false, false);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
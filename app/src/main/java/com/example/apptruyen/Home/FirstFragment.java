package com.example.apptruyen.Home;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apptruyen.R;
import com.example.apptruyen.firebase.ComicAdapter;
import com.example.apptruyen.model.Comic;
import com.example.apptruyen.util.Utility;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FirstFragment extends Fragment {
    private FrameLayout progressOverlay;
    private ProgressBar progressBar;
    private static final String TAG = "FirstFragment";
    private static final String COMICS_JSON_FILE = "comics.json";
    private static final String FIRESTORE_COLLECTION = "comics";
    private static final int DISPLAY_LIMIT = 10;
    private static final String KEY_IS_SHOWING_ALL = "isShowingAll";

    private ComicAdapter adapter;
    private final List<Comic> comicList = new ArrayList<>();
    private final List<Comic> filteredComicList = new ArrayList<>();
    private FirebaseFirestore db;
    private boolean isShowingAll = false;
    private EditText searchEditText;
    private TextView seeAllButton;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        FirebaseApp.initializeApp(context);
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_first, container, false);
        progressOverlay = view.findViewById(R.id.progressOverlay);
        progressBar = view.findViewById(R.id.progressBar);
        setupRecyclerView(view);
        setupSearch(view);
        seeAllButton = view.findViewById(R.id.seeAllNewUpdates);
        setupSeeAllButton(view);
        loadComics();
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            isShowingAll = savedInstanceState.getBoolean(KEY_IS_SHOWING_ALL, false);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateSeeAllButtonText();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_SHOWING_ALL, isShowingAll);
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.newUpdatesRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        adapter = new ComicAdapter(getContext(), filteredComicList);
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch(View view) {
        searchEditText = view.findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            private final Runnable searchRunnable = () -> filterComicsInBackground(searchEditText.getText().toString());
            private final android.os.Handler handler = new android.os.Handler();

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(searchRunnable);
                handler.postDelayed(searchRunnable, 300); // Debounce search
            }
        });
    }

    private void setupSeeAllButton(View view) {
        seeAllButton.setOnClickListener(v -> {
            isShowingAll = !isShowingAll;
            updateSeeAllButtonText();
            filterComicsInBackground(searchEditText.getText().toString());
        });
    }

    private void updateSeeAllButtonText() {
        if (seeAllButton != null) {
            seeAllButton.setText(isShowingAll ? "Thu gọn" : "Xem tất cả");
        }
    }

    private void filterComicsInBackground(String query) {
        showProgress(true);
        new Thread(() -> {
            List<Comic> tempList = query.isEmpty() ? new ArrayList<>(comicList) :
                    comicList.stream()
                            .filter(c -> c.name.toLowerCase().contains(query.toLowerCase()) ||
                                    c.origin_name.toLowerCase().contains(query.toLowerCase()) ||
                                    c.category.stream().anyMatch(cat -> cat.toLowerCase().contains(query.toLowerCase())))
                            .collect(Collectors.toList());

            final List<Comic> finalList = isShowingAll ? tempList :
                    tempList.subList(0, Math.min(DISPLAY_LIMIT, tempList.size()));

            mainHandler.post(() -> {
                filteredComicList.clear();
                filteredComicList.addAll(finalList);
                adapter.notifyDataSetChanged();
                showProgress(false);
                if (!query.isEmpty() && filteredComicList.isEmpty()) {
                    Utility.showToast(getContext(), "Không tìm thấy truyện");
                }
            });
        }).start();
    }

    private void loadComics() {
        showProgress(true);
        db.collection(FIRESTORE_COLLECTION).get().addOnCompleteListener(task -> {
            showProgress(false);
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                parseFirestoreData(task.getResult());
            } else {
                loadFromJson();
            }
        });
    }

    private void loadFromJson() {
        showProgress(true);
        new Thread(() -> {
            try {
                InputStream is = requireContext().getAssets().open(COMICS_JSON_FILE);
                JSONArray items = new JSONObject(new String(is.readAllBytes(), StandardCharsets.UTF_8))
                        .getJSONObject("data").getJSONArray("items");
                is.close();
                parseJsonData(items);
                requireActivity().runOnUiThread(() -> {
                    filterComicsInBackground("");
                    uploadToFirestore();
                    showProgress(false);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Utility.showToast(getContext(), "Lỗi tải dữ liệu JSON");
                    showProgress(false);
                });
            }
        }).start();
    }

    private void uploadToFirestore() {
        if (comicList.isEmpty()) {
            Utility.showToast(getContext(), "Danh sách truyện rỗng");
            return;
        }

        showProgress(true);
        db.collection(FIRESTORE_COLLECTION).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        showProgress(false);
                        Utility.showToast(getContext(), "Lỗi kiểm tra Firestore");
                        return;
                    }

                    List<String> existingIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        existingIds.add(doc.getId());
                    }

                    WriteBatch batch = db.batch();
                    final int uploadCount = (int) comicList.stream()
                            .filter(comic -> !existingIds.contains(comic._id))
                            .map(comic -> batch.set(db.collection(FIRESTORE_COLLECTION).document(comic._id), comic))
                            .count();

                    if (uploadCount == 0) {
                        showProgress(false);
                        Utility.showToast(getContext(), "Không có truyện mới");
                        return;
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                showProgress(false);
                                Utility.showToast(getContext(), "Đã đẩy " + uploadCount + " truyện");
                            })
                            .addOnFailureListener(e -> {
                                showProgress(false);
                                Utility.showToast(getContext(), "Lỗi đẩy dữ liệu: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    showProgress(false);
                    Utility.showToast(getContext(), "Lỗi truy vấn Firestore: " + e.getMessage());
                });
    }

    private void parseJsonData(JSONArray items) throws Exception {
        comicList.clear();
        for (int i = 0; i < items.length(); i++) {
            JSONObject obj = items.getJSONObject(i);
            Comic comic = new Comic();
            comic._id = obj.getString("_id");
            comic.name = obj.getString("name");
            comic.slug = obj.getString("slug");
            comic.origin_name = obj.getJSONArray("origin_name").join(",").replace("\"", "");
            comic.status = obj.getString("status");
            comic.thumb_url = obj.getString("thumb_url");
            comic.updatedAt = obj.getString("updatedAt");
            comic.category = new ArrayList<>();
            JSONArray cats = obj.getJSONArray("category");
            for (int j = 0; j < cats.length(); j++) {
                comic.category.add(cats.getJSONObject(j).getString("name"));
            }
            if (obj.has("chaptersLatest") && !obj.isNull("chaptersLatest") && obj.getJSONArray("chaptersLatest").length() > 0) {
                JSONObject chapObj = obj.getJSONArray("chaptersLatest").getJSONObject(0);
                Comic.Chapter chap = new Comic.Chapter();
                chap.filename = chapObj.optString("filename");
                chap.chapter_name = chapObj.optString("chapter_name");
                chap.chapter_title = chapObj.optString("chapter_title");
                chap.chapter_api_data = chapObj.optString("chapter_api_data");
                comic.latest_chapter = chap;
            }
            comicList.add(comic);
        }
    }

    private void parseFirestoreData(Iterable<QueryDocumentSnapshot> documents) {
        comicList.clear();
        for (QueryDocumentSnapshot doc : documents) {
            Comic comic = new Comic();
            comic._id = doc.getString("_id") != null ? doc.getString("_id") : doc.getId();
            comic.name = doc.getString("name");
            comic.slug = doc.getString("slug");
            comic.origin_name = doc.get("origin_name") instanceof List<?> ?
                    String.join(",", (List<String>) doc.get("origin_name")) : "";
            comic.status = doc.getString("status");
            comic.thumb_url = doc.getString("thumb_url");
            comic.updatedAt = doc.getString("updatedAt");
            comic.category = doc.get("category") instanceof List<?> ?
                    ((List<?>) doc.get("category")).stream()
                            .map(o -> o instanceof Map ? (String) ((Map<?, ?>) o).get("name") : (String) o)
                            .collect(Collectors.toList()) : new ArrayList<>();
            if (doc.get("latest_chapter") instanceof Map) {
                Map<String, Object> chapData = (Map<String, Object>) doc.get("latest_chapter");
                Comic.Chapter chap = new Comic.Chapter();
                chap.filename = (String) chapData.get("filename");
                chap.chapter_name = (String) chapData.get("chapter_name");
                chap.chapter_title = (String) chapData.get("chapter_title");
                chap.chapter_api_data = (String) chapData.get("chapter_api_data");
                comic.latest_chapter = chap;
            }
            comicList.add(comic);
        }
        filterComicsInBackground("");
    }

    private void showProgress(boolean show) {
        progressOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
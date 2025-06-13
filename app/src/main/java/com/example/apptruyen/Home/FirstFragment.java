package com.example.apptruyen.Home;

import android.content.Context;
import android.os.Bundle;
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

    private ComicAdapter adapter;
    private final List<Comic> comicList = new ArrayList<>();
    private final List<Comic> filteredComicList = new ArrayList<>();
    private FirebaseFirestore db;
    private boolean isShowingAll = false;
    private EditText searchEditText;

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
        setupSeeAllButton(view);
        loadComics();
        return view;
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.newUpdatesRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true); // Optimize for fixed-size items
        adapter = new ComicAdapter(getContext(), filteredComicList);
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch(View view) {
        searchEditText = view.findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            private final Runnable searchRunnable = () -> filterComics(searchEditText.getText().toString());
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
        TextView seeAllButton = view.findViewById(R.id.seeAllNewUpdates);
        seeAllButton.setOnClickListener(v -> {
            isShowingAll = !isShowingAll;
            filterComics(searchEditText.getText().toString());
            seeAllButton.setText(isShowingAll ? "Thu gọn" : "Xem tất cả");
        });
    }

    private void filterComics(String query) {
        filteredComicList.clear();
        List<Comic> tempList = query.isEmpty() ? new ArrayList<>(comicList) :
                comicList.stream()
                        .filter(c -> c.name.toLowerCase().contains(query.toLowerCase()) ||
                                c.origin_name.toLowerCase().contains(query.toLowerCase()) ||
                                c.category.stream().anyMatch(cat -> cat.toLowerCase().contains(query.toLowerCase())))
                        .collect(Collectors.toList());
        filteredComicList.addAll(isShowingAll ? tempList :
                tempList.subList(0, Math.min(DISPLAY_LIMIT, tempList.size())));
        adapter.notifyDataSetChanged();
        if (!query.isEmpty() && filteredComicList.isEmpty()) {
            Utility.showToast(getContext(), "Không tìm thấy truyện");
        }
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
                    filterComics("");
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
        if (comicList.isEmpty()) return;
        db.collection(FIRESTORE_COLLECTION).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            List<String> existingIds = task.getResult().getDocuments().stream()
                    .map(doc -> doc.getId()).collect(Collectors.toList());
            int uploadCount = 0;
            for (Comic comic : comicList) {
                if (!existingIds.contains(comic._id)) {
                    db.collection(FIRESTORE_COLLECTION).document(comic._id).set(comic);
                    uploadCount++;
                }
            }
            if (uploadCount > 0) {
                Utility.showToast(getContext(), "Đã đẩy " + uploadCount + " truyện");
            }
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
            if (obj.has("chaptersLatest") && obj.getJSONArray("chaptersLatest").length() > 0) {
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
        filterComics("");
    }

    private void showProgress(boolean show) {
        progressOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
package com.example.apptruyen.Home;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast; // Thêm import Toast

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apptruyen.R;
import com.example.apptruyen.firebase.ComicAdapter;
import com.example.apptruyen.model.Comic;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.HashMap; // Thêm import HashMap cho migrateFirestoreComicIds

public class FirstFragment extends Fragment {

    private static final String TAG = "FirstFragment";
    private static final String COMICS_JSON_FILE = "comics.json";
    private static final String FIRESTORE_COLLECTION = "comics";

    private RecyclerView recyclerView;
    private ComicAdapter adapter;
    private List<Comic> comicList = new ArrayList<>();
    private FirebaseFirestore db;

    // Cờ để điều khiển việc tải dữ liệu (chỉ dùng cho mục đích phát triển/debug)
    // Đặt true để ưu tiên JSON, false để ưu tiên Firestore
    private static final boolean LOAD_FROM_JSON_DEBUG_MODE = false;
    // Đặt true để tải dữ liệu từ JSON lên Firestore MỘT LẦN (chỉ khi LOAD_FROM_JSON_DEBUG_MODE là true)
    private static final boolean UPLOAD_JSON_TO_FIRESTORE_ONCE = false;

    // CỜ ĐỂ CHẠY MIGRATION MỘT LẦN (ĐẶT LÀ TRUE, CHẠY, RỒI ĐẶT LẠI LÀ FALSE)
    private static final boolean RUN_MIGRATION_ONCE = false;


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

        recyclerView = view.findViewById(R.id.newUpdatesRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ComicAdapter(getContext(), comicList);
        recyclerView.setAdapter(adapter);

        // Chạy migration nếu cờ được bật (CHỈ NÊN CHẠY MỘT LẦN)
        if (RUN_MIGRATION_ONCE) {
            Log.d(TAG, "Đang chạy migration để sửa lỗi ID Firestore.");
            migrateFirestoreComicIds();
            // SAU KHI ĐÃ CHẠY VÀ XÁC NHẬN DỮ LIỆU ĐƯỢC SỬA, HÃY ĐẶT RUN_MIGRATION_ONCE VỀ FALSE
            // VÀ GỠ BỎ HOẶC COMMENT DÒNG GỌI NÀY ĐỂ TRÁNH CHẠY LẠI KHÔNG CẦN THIẾT.
        }

        // Gọi phương thức tải dữ liệu chính
        loadComicsData();

        return view;
    }

    /**
     * Phương thức chính để quyết định tải dữ liệu từ đâu.
     * Sử dụng cờ LOAD_FROM_JSON_DEBUG_MODE để chuyển đổi giữa JSON và Firestore.
     */
    private void loadComicsData() {
        if (LOAD_FROM_JSON_DEBUG_MODE) {
            Log.d(TAG, "Đang tải truyện tranh từ JSON (Chế độ Debug)");
            try {
                loadComicsFromJson();
                // Chỉ upload lên Firestore nếu cờ UPLOAD_JSON_TO_FIRESTORE_ONCE là true
                // VÀ chỉ khi danh sách truyện không rỗng
                if (UPLOAD_JSON_TO_FIRESTORE_ONCE && !comicList.isEmpty()) {
                    Log.d(TAG, "Đang tải dữ liệu từ JSON lên Firestore (Chỉ một lần)");
                    uploadComicsToFirestore(comicList);
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Lỗi khi tải truyện tranh từ JSON, chuyển sang Firestore.", e);
                // Nếu JSON lỗi, chuyển sang Firestore làm phương án dự phòng
                loadComicsFromFirestore();
            }
        } else {
            Log.d(TAG, "Đang tải truyện tranh từ Firestore.");
            loadComicsFromFirestore();
        }
    }

    /**
     * Tải truyện tranh từ Firestore và cập nhật giao diện.
     * Xử lý từng trường một cách thủ công để linh hoạt hơn.
     */
    private void loadComicsFromFirestore() {
        db.collection(FIRESTORE_COLLECTION)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        comicList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Comic comic = new Comic();
                                comic._id = document.getString("_id");
                                if (comic._id == null || comic._id.isEmpty()) {
                                    comic._id = document.getId(); // Sử dụng ID của tài liệu Firestore làm fallback
                                    Log.w(TAG, "⚠️ _id field is missing in document " + document.getId() + ", using document ID as _id.");
                                }

                                comic.name = document.getString("name");
                                comic.slug = document.getString("slug");

                                Object originNameField = document.get("origin_name");
                                if (originNameField instanceof List) {
                                    List<String> originNamesList = (List<String>) originNameField;
                                    comic.origin_name = (originNamesList != null && !originNamesList.isEmpty()) ? String.join(",", originNamesList) : "";
                                } else if (originNameField instanceof String) {
                                    comic.origin_name = (String) originNameField;
                                } else {
                                    comic.origin_name = "";
                                    Log.w(TAG, "Unexpected type for origin_name in document " + document.getId() + ": " + (originNameField != null ? originNameField.getClass().getName() : "null"));
                                }

                                comic.status = document.getString("status");
                                comic.thumb_url = document.getString("thumb_url");
                                comic.updatedAt = document.getString("updatedAt");

                                List<String> categories = new ArrayList<>();
                                Object categoryField = document.get("category");
                                if (categoryField instanceof List) {
                                    List<?> rawCategoryList = (List<?>) categoryField;
                                    for (Object item : rawCategoryList) {
                                        if (item instanceof Map) {
                                            Map<String, Object> catMap = (Map<String, Object>) item;
                                            if (catMap.containsKey("name") && catMap.get("name") instanceof String) {
                                                categories.add((String) catMap.get("name"));
                                            }
                                        } else if (item instanceof String) {
                                            categories.add((String) item);
                                        } else {
                                            Log.w(TAG, "Unexpected item type in category list for " + document.getId() + ": " + (item != null ? item.getClass().getName() : "null"));
                                        }
                                    }
                                } else if (categoryField instanceof String) {
                                    categories.add((String) categoryField);
                                    Log.w(TAG, "Category for " + document.getId() + " is a single String: " + categoryField);
                                } else if (categoryField != null) {
                                    Log.w(TAG, "Category field for " + document.getId() + " is not a List or String. Actual type: " + categoryField.getClass().getName());
                                }
                                comic.category = categories;

                                Object latestChapterField = document.get("latest_chapter");
                                if (latestChapterField instanceof Map) {
                                    Map<String, Object> latestChapterMap = (Map<String, Object>) latestChapterField;
                                    Comic.Chapter chap = new Comic.Chapter();
                                    chap.filename = (String) latestChapterMap.get("filename");
                                    chap.chapter_name = (String) latestChapterMap.get("chapter_name");
                                    chap.chapter_title = (String) latestChapterMap.get("chapter_title");
                                    chap.chapter_api_data = (String) latestChapterMap.get("chapter_api_data");
                                    comic.latest_chapter = chap;
                                } else if (latestChapterField != null) {
                                    Log.w(TAG, "latest_chapter for " + document.getId() + " is not a Map. Actual type: " + latestChapterField.getClass().getName() + ". Value: " + latestChapterField);
                                    comic.latest_chapter = null;
                                }

                                comicList.add(comic);
                                Log.d(TAG, "✅ Firestore: " + comic.name);
                            } catch (Exception e) {
                                Log.e(TAG, "❌ Lỗi khi phân tích truyện tranh từ Firestore cho tài liệu: " + document.getId(), e);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.e(TAG, "❌ Lỗi khi tải truyện tranh từ Firestore", task.getException());
                        Log.d(TAG, "Thử tải từ JSON do Firestore lỗi.");
                        try {
                            loadComicsFromJson();
                        } catch (Exception eJson) {
                            Log.e(TAG, "❌ Lỗi khi tải truyện tranh từ JSON sau khi Firestore lỗi.", eJson);
                            Toast.makeText(getContext(), "Không thể tải truyện từ Firestore hoặc tệp cục bộ.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Tải truyện tranh từ tệp assets/comics.json.
     * Phương thức này KHÔNG tự động tải lên Firestore nữa.
     * Để tải lên Firestore, hãy sử dụng cờ UPLOAD_JSON_TO_FIRESTORE_ONCE.
     *
     * @throws Exception nếu đọc hoặc phân tích tệp JSON thất bại
     */
    private void loadComicsFromJson() throws Exception {
        InputStream is = requireContext().getAssets().open(COMICS_JSON_FILE);
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();

        String json = new String(buffer, StandardCharsets.UTF_8);
        JSONObject root = new JSONObject(json);
        JSONArray items = root.getJSONObject("data").getJSONArray("items");

        comicList.clear();

        for (int i = 0; i < items.length(); i++) {
            try {
                JSONObject obj = items.getJSONObject(i);
                Comic comic = new Comic();

                comic._id = obj.getString("_id");
                comic.name = obj.getString("name");
                comic.slug = obj.getString("slug");

                JSONArray originNameArray = obj.getJSONArray("origin_name");
                comic.origin_name = originNameArray.join(",").replace("\"", "");

                comic.status = obj.getString("status");
                comic.thumb_url = obj.getString("thumb_url");
                comic.updatedAt = obj.getString("updatedAt");

                JSONArray catArray = obj.getJSONArray("category");
                List<String> categories = new ArrayList<>();
                for (int j = 0; j < catArray.length(); j++) {
                    try {
                        JSONObject catObj = catArray.getJSONObject(j);
                        categories.add(catObj.getString("name"));
                    } catch (JSONException e) {
                        Log.e(TAG, "❌ Lỗi parse category JSON tại chỉ số " + j, e);
                    }
                }
                comic.category = categories;

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
                Log.d(TAG, "✅ JSON: " + comic.name);

            } catch (JSONException e) {
                Log.e(TAG, "❌ Lỗi khi parse comic từ JSON tại chỉ số " + i, e);
            } catch (Exception e) {
                Log.e(TAG, "❌ Lỗi không mong muốn khi parse comic từ JSON tại chỉ số " + i, e);
            }
        }

        adapter.notifyDataSetChanged();
    }

    /**
     * Tải danh sách truyện tranh lên Firestore.
     * Phương thức này không tự động gọi nữa, mà được gọi thủ công (ví dụ: từ loadComicsData
     * khi UPLOAD_JSON_TO_FIRESTORE_ONCE là true).
     */
    private void uploadComicsToFirestore(List<Comic> comics) {
        for (Comic comic : comics) {
            if (comic._id == null || comic._id.isEmpty()) {
                Log.w(TAG, "Bỏ qua upload comic không có ID: " + comic.name);
                continue;
            }
            db.collection(FIRESTORE_COLLECTION)
                    .document(comic._id)
                    .set(comic)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Upload Firestore: " + comic.name))
                    .addOnFailureListener(e -> Log.e(TAG, "❌ Upload Firestore lỗi: " + comic.name, e));
        }
    }

    /**
     * Phương thức này quét các tài liệu Firestore trong bộ sưu tập "comics"
     * và cập nhật trường "_id" bên trong tài liệu để khớp với ID tài liệu.
     * CHỈ NÊN CHẠY MỘT LẦN hoặc dưới sự kiểm soát chặt chẽ.
     */
    private void migrateFirestoreComicIds() {
        db.collection(FIRESTORE_COLLECTION)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int updatedCount = 0;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String documentId = document.getId();
                            String fieldId = document.getString("_id");

                            // Nếu trường _id bị thiếu, null, rỗng hoặc không khớp với ID tài liệu
                            if (fieldId == null || fieldId.isEmpty() || !fieldId.equals(documentId)) {
                                Log.d(TAG, "Cần cập nhật _id cho tài liệu: " + documentId + ". Current _id field: '" + fieldId + "'");
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("_id", documentId); // Đặt trường _id bằng ID của tài liệu

                                document.getReference().update(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "✅ Đã cập nhật _id cho tài liệu: " + documentId);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "❌ Lỗi cập nhật _id cho tài liệu: " + documentId, e);
                                        });
                                updatedCount++;
                            }
                        }
                        if (updatedCount > 0) {
                            Toast.makeText(getContext(), "Đã quét và cập nhật " + updatedCount + " tài liệu.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(), "Không tìm thấy tài liệu nào cần cập nhật _id.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "❌ Lỗi khi tải tài liệu để migration", task.getException());
                        Toast.makeText(getContext(), "Lỗi khi chạy migration.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
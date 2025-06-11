    package com.example.apptruyen.Home;

    import android.content.Context;
    import android.os.Bundle;
    import android.text.Editable;
    import android.text.TextWatcher;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.EditText;
    import android.widget.Toast;

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
    import org.json.JSONObject;

    import java.io.InputStream;
    import java.nio.charset.StandardCharsets;
    import java.util.ArrayList;
    import java.util.List;
    import java.util.Map;
    import java.util.stream.Collectors;
    
    public class FirstFragment extends Fragment {

        private static final String TAG = "FirstFragment"; // Thẻ log
        private static final String COMICS_JSON_FILE = "comics.json"; // Tệp JSON
        private static final String FIRESTORE_COLLECTION = "comics"; // Bộ sưu tập Firestore

        private ComicAdapter adapter; // Adapter cho RecyclerView
        private final List<Comic> comicList = new ArrayList<>(); // Danh sách truyện
        private final List<Comic> filteredComicList = new ArrayList<>(); // Danh sách lọc
        private FirebaseFirestore db; // Firestore instance

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            FirebaseApp.initializeApp(context); // Khởi tạo Firebase
            db = FirebaseFirestore.getInstance(); // Khởi tạo Firestore
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_first, container, false); // Tải layout
            RecyclerView recyclerView = view.findViewById(R.id.newUpdatesRecycler);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new ComicAdapter(getContext(), filteredComicList); // Gắn adapter
            recyclerView.setAdapter(adapter);

            // Theo dõi thay đổi văn bản tìm kiếm
            ((EditText) view.findViewById(R.id.searchEditText)).addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) { filterComics(s.toString()); }
            });

            loadComics(); // Tải dữ liệu
            return view;
        }

        // Lọc truyện theo truy vấn
        private void filterComics(String query) {
            filteredComicList.clear();
            filteredComicList.addAll(query.isEmpty() ? comicList :
                    comicList.stream().filter(c -> c.name.toLowerCase().contains(query.toLowerCase()) ||
                                    c.origin_name.toLowerCase().contains(query.toLowerCase()) ||
                                    c.category.stream().anyMatch(cat -> cat.toLowerCase().contains(query.toLowerCase())))
                            .collect(Collectors.toList()));
            adapter.notifyDataSetChanged(); // Cập nhật UI
            if (!query.isEmpty() && filteredComicList.isEmpty()) showToast("Không tìm thấy truyện");
        }

        // Tải truyện từ Firestore
        private void loadComics() {
            db.collection(FIRESTORE_COLLECTION).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    updateComicLists(task.getResult()); // Cập nhật danh sách
                    Log.d(TAG, "✅ Tải " + comicList.size() + " truyện từ Firestore");
                } else {
                    error("Firestore", task.getException()); // Xử lý lỗi
                    loadAndUploadFromJson(); // Tải từ JSON
                }
            });
        }

        // Tải và đẩy dữ liệu từ JSON
        private void loadAndUploadFromJson() {
            try {
                // Đọc JSON từ assets
                InputStream is = requireContext().getAssets().open(COMICS_JSON_FILE);
                JSONArray items = new JSONObject(new String(is.readAllBytes(), StandardCharsets.UTF_8))
                        .getJSONObject("data").getJSONArray("items");
                is.close();

                comicList.clear();
                filteredComicList.clear();
                for (int i = 0; i < items.length(); i++) {
                    Comic comic = parseJsonComic(items.getJSONObject(i));
                    comicList.add(comic);
                    filteredComicList.add(comic);
                }
                adapter.notifyDataSetChanged();
                Log.d(TAG, "✅ Tải " + comicList.size() + " truyện từ JSON");

                uploadComicsToFirestore(); // Đẩy lên Firestore
            } catch (Exception e) {
                error("JSON", e);
            }
        }

        // Đẩy truyện mới lên Firestore
        private void uploadComicsToFirestore() {
            if (comicList.isEmpty()) {
                Log.w(TAG, "⚠️ Danh sách truyện rỗng");
                return;
            }

            db.collection(FIRESTORE_COLLECTION).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<String> existingIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) existingIds.add(doc.getId());

                    int uploadCount = 0;
                    for (Comic comic : comicList) {
                        if (!existingIds.contains(comic._id)) {
                            db.collection(FIRESTORE_COLLECTION).document(comic._id).set(comic)
                                    .addOnSuccessListener(v -> Log.d(TAG, "✅ Tải lên: " + comic.name))
                                    .addOnFailureListener(e -> Log.e(TAG, "❌ Lỗi tải lên: " + comic.name, e));
                            uploadCount++;
                        }
                    }
                    if (uploadCount > 0) showToast("Đã đẩy " + uploadCount + " truyện lên Firestore");
                    else Log.d(TAG, "✅ Không có truyện mới");
                } else {
                    Log.e(TAG, "❌ Lỗi kiểm tra Firestore", task.getException());
                }
            });
        }

        // Phân tích truyện từ JSON
        private Comic parseJsonComic(JSONObject obj) throws Exception {
            Comic comic = new Comic();
            comic._id = obj.getString("_id"); // ID
            comic.name = obj.getString("name"); // Tên
            comic.slug = obj.getString("slug"); // Slug
            comic.origin_name = obj.getJSONArray("origin_name").join(",").replace("\"", ""); // Tên gốc
            comic.status = obj.getString("status"); // Trạng thái
            comic.thumb_url = obj.getString("thumb_url"); // Ảnh bìa
            comic.updatedAt = obj.getString("updatedAt"); // Thời gian cập nhật
            comic.category = new ArrayList<>();
            JSONArray cats = obj.getJSONArray("category");
            for (int i = 0; i < cats.length(); i++) comic.category.add(cats.getJSONObject(i).getString("name")); // Danh mục

            // Xử lý chương mới nhất
            if (obj.has("chaptersLatest") && obj.getJSONArray("chaptersLatest").length() > 0) {
                JSONObject chapObj = obj.getJSONArray("chaptersLatest").getJSONObject(0);
                Comic.Chapter chap = new Comic.Chapter();
                chap.filename = chapObj.optString("filename");
                chap.chapter_name = chapObj.optString("chapter_name");
                chap.chapter_title = chapObj.optString("chapter_title");
                chap.chapter_api_data = chapObj.optString("chapter_api_data");
                comic.latest_chapter = chap;
            }
            return comic;
        }

        // Phân tích truyện từ Firestore
        private Comic parseFirestoreComic(QueryDocumentSnapshot doc) {
            Comic comic = new Comic();
            comic._id = doc.getString("_id") != null ? doc.getString("_id") : doc.getId(); // ID
            comic.name = doc.getString("name"); // Tên
            comic.slug = doc.getString("slug"); // Slug
            comic.origin_name = parseStringList(doc.get("origin_name")); // Tên gốc
            comic.status = doc.getString("status"); // Trạng thái
            comic.thumb_url = doc.getString("thumb_url"); // Ảnh bìa
            comic.updatedAt = doc.getString("updatedAt"); // Thời gian cập nhật
            comic.category = parseCategoryList(doc.get("category")); // Danh mục

            // Xử lý chương mới nhất
            if (doc.get("latest_chapter") instanceof Map) {
                Map<String, Object> chapData = (Map<String, Object>) doc.get("latest_chapter");
                Comic.Chapter chap = new Comic.Chapter();
                chap.filename = (String) chapData.get("filename");
                chap.chapter_name = (String) chapData.get("chapter_name");
                chap.chapter_title = (String) chapData.get("chapter_title");
                chap.chapter_api_data = (String) chapData.get("chapter_api_data");
                comic.latest_chapter = chap;
            }
            return comic;
        }

        // Chuyển danh mục thành danh sách chuỗi
        private List<String> parseCategoryList(Object field) {
            List<String> list = new ArrayList<>();
            if (field instanceof List<?>) {
                for (Object o : (List<?>) field) list.add(o instanceof Map ? (String) ((Map<?, ?>) o).get("name") : (String) o);
            } else if (field instanceof String) list.add((String) field);
            return list;
        }

        // Chuyển origin_name thành chuỗi
        private String parseStringList(Object field) {
            return field instanceof List<?> ? String.join(",", (List<String>) field) : "";
        }

        // Cập nhật danh sách truyện
        private void updateComicLists(Iterable<QueryDocumentSnapshot> documents) {
            comicList.clear();
            filteredComicList.clear();
            for (QueryDocumentSnapshot doc : documents) {
                Comic comic = parseFirestoreComic(doc);
                comicList.add(comic);
                filteredComicList.add(comic);
            }
            adapter.notifyDataSetChanged();
        }

        // Xử lý lỗi
        private void error(String source, Exception e) {
            Log.e(TAG, "Lỗi tải từ " + source, e);
            showToast("Lỗi tải dữ liệu từ " + source);
        }

        // Hiển thị Toast
        private void showToast(String message) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
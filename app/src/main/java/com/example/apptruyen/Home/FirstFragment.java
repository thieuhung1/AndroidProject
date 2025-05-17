package com.example.apptruyen.Home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FirstFragment extends Fragment {

    private RecyclerView recyclerView;
    private ComicAdapter adapter;
    private List<Comic> comicList = new ArrayList<>();
    private void uploadComicsToFirestore(List<Comic> comics) {
        FirebaseApp.initializeApp(requireContext()); // ✅ Gọi đúng trong Fragment
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (Comic comic : comics) {
            db.collection("comics")
                    .document(comic.id) // sử dụng _id làm document ID
                    .set(comic)
                    .addOnSuccessListener(aVoid ->
                            Log.d("FIRESTORE", "✅ Đã upload: " + comic.name))
                    .addOnFailureListener(e ->
                            Log.e("FIRESTORE", "❌ Lỗi khi upload: " + comic.name, e));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_first, container, false);
        recyclerView = view.findViewById(R.id.newUpdatesRecycler);

        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ComicAdapter(getContext(), comicList);
        recyclerView.setAdapter(adapter);

        loadComicsFromJson();
        return view;
    }

    private void loadComicsFromJson() {
        try {
            InputStream is = requireContext().getAssets().open("comics.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(json);
            JSONArray items = root.getJSONObject("data").getJSONArray("items");
            int limit = Math.min(items.length(), 3);

            for (int i = 0; i < items.length(); i++) {
                JSONObject obj = items.getJSONObject(i);
                Comic comic = new Comic();
                comic.id = obj.getString("_id");
                comic.name = obj.getString("name");
                comic.slug = obj.getString("slug");
                comic.origin_name = obj.getJSONArray("origin_name").join(",").replace("\"", "");
                comic.status = obj.getString("status");
                comic.thumb_url = obj.getString("thumb_url");
                comic.updatedAt = obj.getString("updatedAt");

                comic.category = new ArrayList<>();
                JSONArray catArray = obj.getJSONArray("category");
                for (int j = 0; j < catArray.length(); j++) {
                    comic.category.add(catArray.getJSONObject(j).getString("name"));
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

            adapter.notifyDataSetChanged();
            uploadComicsToFirestore(comicList);

        } catch (Exception e) {
            Log.e("JSON_ERROR", "Lỗi khi load comics", e);
        }



    }
}

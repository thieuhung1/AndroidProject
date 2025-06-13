package com.example.apptruyen.Home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apptruyen.R;
import com.example.apptruyen.firebase.ComicAdapter;
import com.example.apptruyen.model.Comic;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class ThirdFragment extends Fragment {
    private RecyclerView recyclerLikedComics;
    private ComicAdapter comicAdapter;
    private final List<Comic> likedComicsList = new ArrayList<>();
    private FirebaseFirestore db;
    private SharedPreferences prefs;
    private Context ctx;
    private String username; // tên người dùng hiện tại

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_third, container, false);
        ctx = requireContext();

        // Khởi tạo RecyclerView
        recyclerLikedComics = view.findViewById(R.id.recyclerLC);
        comicAdapter = new ComicAdapter(ctx, likedComicsList);
        recyclerLikedComics.setLayoutManager(new GridLayoutManager(ctx, 3));
        recyclerLikedComics.setAdapter(comicAdapter);

        db = FirebaseFirestore.getInstance();

        // ✅ Lấy username từ SharedPreferences (user đã đăng nhập)
        SharedPreferences userPrefs = ctx.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        username = userPrefs.getString("username", null);

        if (username == null) {
            Toast.makeText(ctx, "Không xác định được người dùng", Toast.LENGTH_SHORT).show();
            return view; // không load nếu chưa đăng nhập
        }

        // ✅ Mỗi người dùng có 1 SharedPreferences riêng
        prefs = ctx.getSharedPreferences("FavoriteComics_" + username, Context.MODE_PRIVATE);

        loadLikedComics(); // tải danh sách truyện yêu thích
        return view;
    }

    private void loadLikedComics() {
        Set<String> favorites = prefs.getStringSet("favorite_comics", new HashSet<>());
        if (favorites.isEmpty()) {
            Toast.makeText(ctx, "Chưa có truyện yêu thích", Toast.LENGTH_SHORT).show();
            return;
        }

        likedComicsList.clear();
        final int[] loaded = {0}; // đếm số lượng đã tải

        for (String id : favorites) {
            db.collection("comics").document(id).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Comic c = new Comic();
                            c._id = doc.getId();
                            c.name = doc.getString("name");
                            c.author = doc.getString("author");
                            c.description = doc.getString("description");
                            c.thumb_url = doc.getString("thumb_url");
                            c.slug = doc.getString("slug");
                            c.status = doc.getString("status");
                            c.category = (List<String>) doc.get("category");
                            likedComicsList.add(c);
                        }

                        if (++loaded[0] == favorites.size()) {
                            comicAdapter.notifyDataSetChanged(); // chỉ update khi load đủ
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ctx, "Lỗi tải truyện " + id, Toast.LENGTH_SHORT).show();
                        if (++loaded[0] == favorites.size()) {
                            comicAdapter.notifyDataSetChanged();
                        }
                    });
        }
    }
}

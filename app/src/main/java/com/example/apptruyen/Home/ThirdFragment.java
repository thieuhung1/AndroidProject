package com.example.apptruyen.Home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.apptruyen.R;
import com.example.apptruyen.firebase.ComicAdapter;
import com.example.apptruyen.model.Comic;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ThirdFragment extends Fragment {
    private RecyclerView recyclerLikedComics;
    private ComicAdapter comicAdapter;
    private final List<Comic> likedComicsList = new ArrayList<>();
    private FirebaseFirestore db;
    private SharedPreferences prefs;
    private Context ctx;
    private String username;
    private FrameLayout progressOverlay;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_third, container, false);
        ctx = requireContext();

        // Initialize ProgressBar programmatically
        progressOverlay = new FrameLayout(ctx);
        progressOverlay.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        progressOverlay.setBackgroundColor(0x80000000); // Semi-transparent background
        progressBar = new ProgressBar(ctx);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = android.view.Gravity.CENTER;
        progressBar.setLayoutParams(params);
        progressOverlay.addView(progressBar);
        ((FrameLayout) view).addView(progressOverlay);
        progressOverlay.setVisibility(View.GONE);

        // Initialize RecyclerView
        recyclerLikedComics = view.findViewById(R.id.recyclerLC);
        recyclerLikedComics.setLayoutManager(new GridLayoutManager(ctx, 3));
        recyclerLikedComics.setHasFixedSize(true); // Optimize for fixed-size items
        comicAdapter = new ComicAdapter(ctx, likedComicsList);
        recyclerLikedComics.setAdapter(comicAdapter);

        db = FirebaseFirestore.getInstance();

        // Get username from SharedPreferences
        SharedPreferences userPrefs = ctx.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        username = userPrefs.getString("username", null);

        if (username == null) {
            Toast.makeText(ctx, "Không xác định được người dùng", Toast.LENGTH_SHORT).show();
            return view;
        }

        prefs = ctx.getSharedPreferences("FavoriteComics_" + username, Context.MODE_PRIVATE);
        loadLikedComics();
        return view;
    }

    private void loadLikedComics() {
        Set<String> favorites = prefs.getStringSet("favorite_comics", new HashSet<>());
        if (favorites.isEmpty()) {
            Toast.makeText(ctx, "Chưa có truyện yêu thích", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);
        likedComicsList.clear();
        final int[] loaded = {0};
        int total = favorites.size();

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

                        if (++loaded[0] == total) {
                            comicAdapter.notifyDataSetChanged();
                            recyclerLikedComics.startAnimation(AnimationUtils.loadAnimation(ctx, R.anim.fade_in));
                            showProgress(false);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ctx, "Lỗi tải truyện " + id, Toast.LENGTH_SHORT).show();
                        if (++loaded[0] == total) {
                            comicAdapter.notifyDataSetChanged();
                            recyclerLikedComics.startAnimation(AnimationUtils.loadAnimation(ctx, R.anim.fade_in));
                            showProgress(false);
                        }
                    });
        }
    }

    private void showProgress(boolean show) {
        progressOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
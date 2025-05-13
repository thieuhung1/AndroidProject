package com.example.apptruyen.Home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apptruyen.ComicDataLoader;
import com.example.apptruyen.R;
import com.example.apptruyen.firebase.ComicAdapter;
import com.example.apptruyen.model.Manga;

import java.util.ArrayList;
import java.util.List;

public class FirstFragment extends Fragment {

    private RecyclerView recyclerView;
    private ComicAdapter adapter;
    private List<Manga> comicList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_first, container, false);

        recyclerView = view.findViewById(R.id.newUpdatesRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        comicList = new ArrayList<>();
        adapter = new ComicAdapter(getContext(), comicList);
        recyclerView.setAdapter(adapter);

        comicList.addAll(ComicDataLoader.loadComics(getContext(), "response_1747039077763.json"));
        adapter.notifyDataSetChanged();

        return view;
    }
}
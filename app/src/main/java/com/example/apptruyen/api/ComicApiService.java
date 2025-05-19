package com.example.apptruyen.api;

import com.example.apptruyen.firebase.ChapterAdapter;
import com.example.apptruyen.model.Comic;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Url;

public interface ComicApiService {
    @GET("chapters/{comicId}")  // Kiểm tra đường dẫn endpoint
    Call<List<Comic.Chapter>> getChaptersList(@Path("comicId") String comicId);

//    @GET("v1/api/chapter/{chapterId}")
//    Call<ChapterResponse> getChapter(@Path("chapterId") String chapterId);
}
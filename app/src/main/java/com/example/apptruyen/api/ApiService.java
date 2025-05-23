package com.example.apptruyen.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    @GET("v1/api/chapter/{chapterId}")
    Call<ChapterResponse> getChapterContent(@Path("chapterId") String chapterId);
}
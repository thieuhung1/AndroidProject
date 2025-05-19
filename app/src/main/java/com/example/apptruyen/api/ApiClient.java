package com.example.apptruyen.api;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class ApiClient {
    private static final OkHttpClient client = new OkHttpClient();

    public static void get(String url, Callback callback) {

        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(callback);
    }

}
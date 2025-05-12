package com.example.apptruyen.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Manga {
    @SerializedName("_id") // Ánh xạ với trường "_id" trong JSON
    private String id;
    private String name;
    private String slug;

    @SerializedName("origin_name")
    private List<String> originName;

    private String status;

    @SerializedName("thumb_url")
    private String thumbUrl;

    @SerializedName("sub_docquyen")
    private boolean subDocQuyen;

    private List<Category> category; // Khớp với "category" trong JSON

    @SerializedName("updatedAt")
    private String updatedAt;

    @SerializedName("chaptersLatest")
    private List<Chapter> latestChapters;

    // Constructor, Getters, Setters (generate tự động)
    public String getId() {
        return id; // Hoặc _id nếu dùng @SerializedName("_id")
    }

    public String getName() {
        return name;
    }
}

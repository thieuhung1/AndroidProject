package com.example.apptruyen.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChapterResponse {
    @SerializedName("status")
    public String status;

    @SerializedName("message")
    public String message;

    @SerializedName("data")
    public Data data;

    public static class Data {
        @SerializedName("domain_cdn")
        public String domainCdn;

        @SerializedName("item")
        public Item item;
    }

    public static class Item {
        @SerializedName("chapter_path")
        public String chapterPath;

        @SerializedName("chapter_image")
        public List<Image> chapterImage;
    }

    public static class Image {
        @SerializedName("image_file")
        public String imageFile;
    }
}
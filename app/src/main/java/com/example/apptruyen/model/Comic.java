package com.example.apptruyen.model;

import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.List;

public class Comic implements Serializable {
    @PropertyName("_id")
    public String _id;

    public String name;
    public String slug;
    public String origin_name;
    public String status;
    public String thumb_url;
    public String updatedAt;
    public List<String> category;
    public Chapter latest_chapter;
    public String author;
    public String description;

    public static class Chapter implements Serializable {
        public String filename;
        public String chapter_name;
        public String chapter_title;
        public String chapter_api_data;
    }

    public static class Category implements Serializable {
        public String id;
        public String name;
        public String slug;
    }
}

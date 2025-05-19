package com.example.apptruyen.model;

import java.io.Serializable;
import java.util.List;

public class Comic implements Serializable {
    public String _id;
    public String name;
    public String slug;
    public String origin_name;
    public String status;
    public String thumb_url;
    public String updatedAt;
    public List<String> category;
    public Chapter latest_chapter;

    public static class Chapter implements Serializable {
        public String filename;
        public String chapter_name;
        public String chapter_title;
        public String chapter_api_data;
        public String chapter_id;
    public static class Category implements Serializable {
            public String id;
            public String name;
            public String slug;
        }
    }
}

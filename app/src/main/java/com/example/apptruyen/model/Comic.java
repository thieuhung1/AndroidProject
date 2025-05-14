package com.example.apptruyen.model;

import java.util.List;

public class Comic {
    public String id;
    public String name;
    public String slug;
    public String origin_name;
    public String status;
    public String thumb_url;
    public String updatedAt;
    public List<String> category;
    public Chapter latest_chapter;

    public static class Chapter {
        public String filename;
        public String chapter_name;
        public String chapter_title;
        public String chapter_api_data;
    }
}

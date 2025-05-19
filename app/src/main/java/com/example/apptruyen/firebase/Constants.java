package com.example.apptruyen.firebase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
public class Constants {
    //user
    public static final String KEY_COLLECTION_USERS = "users";
    public static final String KEY_NAME = "name";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PASSWORD = "password";
    //comic
    public static final String KEY_COLLECTION_COMICS = "comics";
    public static final String KEY_NAME_COMIC = "name";
    public static final String KEY_SLUG = "slug";
    public static final String KEY_ORIGIN_NAME = "origin_name";
    public static final String KEY_STATUS = "status";
    public static final String KEY_THUMB_URL = "thumb_url";
    public static final String KEY_UPDATED_AT = "updatedAt";
    public static final String KEY_CATEGORY = "category";
    public static final String KEY_LATEST_CHAPTER = "latest_chapter";
    // chapter
    public static final String KEY_COLLECTION_CHAPTERS = "chapters";
    public static final String KEY_FILENAME = "filename";
    public static final String KEY_CHAPTER_NAME = "chapter_name";
    public static final String KEY_CHAPTER_TITLE = "chapter_title";
    public static final String KEY_CHAPTER_API_DATA = "chapter_api_data";
    public static final String KEY_CHAPTER_ID = "chapter_id";
    //chapter content
    public static final String KEY_COLLECTION_CHAPTER_CONTENT = "chapter_content";
    public static final String KEY_PAGE_URLS = "page_urls";
    public static final String KEY_CHAPTER_CONTENT = "chapter_name";
    public static final String KEY_NEXT_CHAPTER_ID = "next_chapter_id";
    public static final String KEY_PREV_CHAPTER_ID = "prev_chapter_id";


}

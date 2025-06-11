package com.example.apptruyen.util;

import android.content.Context;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.module.AppGlideModule;

@GlideModule
public class MyAppGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        // Tính toán kích thước bộ nhớ cache dựa trên kích thước màn hình
        MemorySizeCalculator calculator = new MemorySizeCalculator.Builder(context)
                .setMemoryCacheScreens(2) // Sử dụng bộ nhớ cache cho 2 màn hình
                .build();

        // Thiết lập bộ nhớ cache cho Glide
        builder.setMemoryCache(new LruResourceCache(calculator.getMemoryCacheSize()));
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false; // Tắt phân tích manifest để tăng tốc độ khởi động
    }
}
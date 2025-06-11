package com.example.apptruyen.Home;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.apptruyen.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Home extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private Fragment firstFragment, secondFragment, thirdFragment, fourFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Khởi tạo Fragment
        firstFragment = new FirstFragment();
        secondFragment = new SecondFragment();
        thirdFragment = new ThirdFragment();
        fourFragment = new FourFragment();

        // Đặt Fragment mặc định
        setCurrentFragment(firstFragment);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.home) {
                setCurrentFragment(firstFragment);
            } else if (id == R.id.profile) {
                setCurrentFragment(secondFragment);
            } else if (id == R.id.favorites) {
                setCurrentFragment(thirdFragment);
            } else if (id == R.id.categoriesTitle) {
                setCurrentFragment(fourFragment);
            }
            return true;
        });
    }

    private void setCurrentFragment(Fragment fragment) {
        Log.d(TAG, "Switching to fragment: " + fragment.getClass().getSimpleName());
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(0, 0) // Tắt animation để tránh lỗi Window
                .replace(R.id.flFragment, fragment)
                .commit();
    }
}
package com.example.apptruyen.Home;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.apptruyen.Home.FirstFragment;
import com.example.apptruyen.Home.SecondFragment;
import com.example.apptruyen.Home.ThirdFragment;
import com.example.apptruyen.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Home extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        Fragment firstFragment = new FirstFragment();
        Fragment secondFragment = new SecondFragment();
        Fragment thirdFragment = new ThirdFragment();
        Fragment fourFragment = new FourFragment();

        setCurrentFragment(firstFragment);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.home) {
                setCurrentFragment(firstFragment);
            } else if (id == R.id.profile) {
                setCurrentFragment(secondFragment);
            } else if (id == R.id.favorites) {
                setCurrentFragment(thirdFragment);
            }else if (id == R.id.categoriesTitle) {
                setCurrentFragment(fourFragment);
            }
            return true;
        });
    }

    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flFragment, fragment)
                .commit();
    }
}
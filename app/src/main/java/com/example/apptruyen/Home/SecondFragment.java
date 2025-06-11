package com.example.apptruyen.Home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.apptruyen.Login.Login;
import com.example.apptruyen.R;
import com.google.firebase.auth.FirebaseAuth;

public class SecondFragment extends Fragment {
    private static final String TAG = "SecondFragment";
    private Button logoutButton;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_second, container, false);

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ nút đăng xuất
        logoutButton = view.findViewById(R.id.logout_button);

        // Thiết lập sự kiện nhấn nút đăng xuất
        logoutButton.setOnClickListener(v -> {
            if (isAdded()) {
                logout();
            }
        });

        return view;
    }

    private void logout() {
        Log.d(TAG, "Logging out");
        mAuth.signOut();

        // Xóa cache SharedPreferences
        if (getActivity() != null) {
            getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();
        }

        // Chuyển về màn hình đăng nhập
        if (isAdded() && getActivity() != null) {
            Intent intent = new Intent(getActivity(), Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called");
    }
}
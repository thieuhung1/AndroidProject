package com.example.apptruyen.Home;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.apptruyen.Login.Login;
import com.example.apptruyen.R;
import com.example.apptruyen.register.register;


public class SecondFragment extends Fragment {
    Button logoutButton;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_second, container, false);

        // Tìm nút đăng xuất
        logoutButton = view.findViewById(R.id.logout_button);

        // Set sự kiện click cho nút Đăng xuất
        logoutButton.setOnClickListener(v -> logout());

        return view;
    }
    private void logout() {
        // Xóa thông tin đăng nhập (ví dụ: xóa session, token, sharedPreferences, v.v.)
        // Ví dụ với SharedPreferences
        getActivity().getSharedPreferences("user_prefs", getContext().MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        // Chuyển hướng về màn hình login
        Intent intent = new Intent(getActivity(), Login.class);
        startActivity(intent);
        getActivity().finish(); // Đóng màn hình hiện tại nếu không cần quay lại
    }

}
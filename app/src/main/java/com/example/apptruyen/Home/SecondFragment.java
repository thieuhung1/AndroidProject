package com.example.apptruyen.Home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.apptruyen.Login.Login;
import com.example.apptruyen.R;
import com.example.apptruyen.Home.EditAccountActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SecondFragment extends Fragment {
    private static final String TAG = "SecondFragment";

    private Button logoutButton, settingButton;
    private TextView usernameTextView, emailTextView;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_second, container, false);

        logoutButton = view.findViewById(R.id.logout_button);
        settingButton = view.findViewById(R.id.buttons);
        usernameTextView = view.findViewById(R.id.textView);     // Tên người dùng
        emailTextView = view.findViewById(R.id.textView3);       // Email

        db = FirebaseFirestore.getInstance();

        // Đọc username từ SharedPreferences
        SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String username = prefs.getString("username", null);

        if (username != null) {
            db.collection("users").document(username).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("username");
                            String email = documentSnapshot.getString("email");

                            usernameTextView.setText(name);
                            emailTextView.setText(email);
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Lỗi lấy dữ liệu người dùng", e));
        }

        logoutButton.setOnClickListener(v -> logout());

        settingButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditAccountActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void logout() {
        if (getActivity() != null) {
            getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();

            Intent intent = new Intent(getActivity(), Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }
}

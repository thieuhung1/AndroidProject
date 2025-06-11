package com.example.apptruyen.Home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.AppCompatButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.apptruyen.Login.Login;
import com.example.apptruyen.R;
import com.example.apptruyen.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class SecondFragment extends Fragment {
    private ImageView profileImage;
    private TextView usernameText, emailText;
    private AppCompatButton settingsButton, storyShelfButton, accountInfoButton, notificationsButton, logoutButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_second, container, false);

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Ánh xạ các thành phần giao diện
        profileImage = view.findViewById(R.id.imageView7);
        usernameText = view.findViewById(R.id.textView);
        emailText = view.findViewById(R.id.textView3);
        settingsButton = view.findViewById(R.id.button);
        storyShelfButton = view.findViewById(R.id.button1);
        accountInfoButton = view.findViewById(R.id.button2);
        notificationsButton = view.findViewById(R.id.button3);
        logoutButton = view.findViewById(R.id.logout_button);

        // Tải dữ liệu người dùng
        loadUserProfile();

        // Thiết lập sự kiện nhấn nút
//        setupButtonListeners();

        return view;
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference userRef = db.collection("users").document(userId);

            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Ánh xạ dữ liệu Firestore vào đối tượng User
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        usernameText.setText(user.getUsername());
                        emailText.setText(user.getEmail());

                        // Tải ảnh đại diện (nếu có)
                        String profileImageUrl = documentSnapshot.getString("img");
                        Glide.with(this)
                                .load(profileImageUrl != null && !profileImageUrl.isEmpty() ? profileImageUrl : R.drawable.ic_profilepic)
                                .circleCrop()
                                .placeholder(R.drawable.ic_profilepic)
                                .into(profileImage);
                    }
                } else {
                    Toast.makeText(getContext(), "Không tìm thấy dữ liệu người dùng", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Lỗi khi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            // Người dùng chưa đăng nhập, chuyển về màn hình đăng nhập
            Intent intent = new Intent(getActivity(), Login.class);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        }
    }

//    private void setupButtonListeners() {
//        settingsButton.setOnClickListener(v -> {
//            // Chuyển đến màn hình cài đặt
//            Intent intent = new Intent(getActivity(), SettingsActivity.class);
//            startActivity(intent);
//        });
//
//        storyShelfButton.setOnClickListener(v -> {
//            // Chuyển đến màn hình tủ truyện
//            Intent intent = new Intent(getActivity(), StoryShelfActivity.class);
//            startActivity(intent);
//        });
//
//        accountInfoButton.setOnClickListener(v -> {
//            // Chuyển đến màn hình thông tin tài khoản
//            Intent intent = new Intent(getActivity(), AccountInfoActivity.class);
//            startActivity(intent);
//        });
//
//        notificationsButton.setOnClickListener(v -> {
//            // Chuyển đến màn hình thông báo
//            Intent intent = new Intent(getActivity(), NotificationsActivity.class);
//            startActivity(intent);
//        });
//
//        logoutButton.setOnClickListener(v -> logout());
//    }

    private void logout() {
        // Đăng xuất khỏi Firebase
        mAuth.signOut();

        // Xóa SharedPreferences (nếu dùng làm dự phòng)
        if (getActivity() != null) {
            getActivity().getSharedPreferences("user_prefs", getContext().MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();
        }

        // Chuyển về màn hình đăng nhập
        Intent intent = new Intent(getActivity(), Login.class);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
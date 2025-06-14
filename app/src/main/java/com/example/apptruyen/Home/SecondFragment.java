package com.example.apptruyen.Home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.apptruyen.Login.Login;
import com.example.apptruyen.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class SecondFragment extends Fragment {
    private static final String TAG = "SecondFragment";

    private AppCompatButton btnLogout, btnEditProfile;
    private TextView tvUserName, tvUserEmail;
    private ImageView ivProfilePicture;
    private FirebaseFirestore db;
    private ListenerRegistration userListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_second, container, false);

        // Initialize views
        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        ivProfilePicture = view.findViewById(R.id.ivAvatar);

        db = FirebaseFirestore.getInstance();

        // Load username from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String username = prefs.getString("username", null);

        if (username != null) {
            loadUserDataRealtime(username);
        } else {
            Log.e(TAG, "Username not found in SharedPreferences.");
        }

        // Set click listeners
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditAccountActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void loadUserDataRealtime(String username) {
        userListener = db.collection("users").document(username)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening for user data", e);
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("username");
                        String email = documentSnapshot.getString("email");
                        String profilePictureUrl = documentSnapshot.getString("profilePictureUrl");

                        // Update UI
                        tvUserName.setText(name);
                        tvUserEmail.setText(email);

                        // Load profile picture with Glide
                        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profilePictureUrl)
                                    .placeholder(R.drawable.ic_profilepic)
                                    .error(R.drawable.ic_profilepic)
                                    .into(ivProfilePicture);
                        } else {
                            ivProfilePicture.setImageResource(R.drawable.ic_profilepic);
                        }
                    } else {
                        Log.e(TAG, "No document found for user: " + username);
                    }
                });
    }

    private void showLogoutConfirmation() {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Có", (dialog, which) -> logout())
                .setNegativeButton("Không", null)
                .show();
    }

    private void logout() {
        if (getActivity() == null) return;

        getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        Intent intent = new Intent(getActivity(), Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) {
            userListener.remove();
        }
    }
}
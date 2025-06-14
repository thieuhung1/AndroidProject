package com.example.apptruyen.Home;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.apptruyen.Login.Login;
import com.example.apptruyen.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SecondFragment extends Fragment {
    private static final String TAG = "SecondFragment";
    private AppCompatButton btnLogout, btnEditProfile, btnChangePassword;
    private TextView tvUserName, tvUserEmail;
    private CircleImageView ivProfilePicture;
    private FirebaseFirestore db;
    private ListenerRegistration userListener;
    private String currentUsername;
    private String encodedImage;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openImagePicker();
                } else {
                    showToast("Cần cấp quyền để chọn ảnh.");
                }
            }
    );

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        InputStream inputStream = getContext().getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        ivProfilePicture.setImageBitmap(bitmap);
                        encodedImage = encodeImage(bitmap);
                        if (encodedImage != null) {
                            updateUserImage(encodedImage);
                        }
                        if (inputStream != null) inputStream.close();
                    } catch (Exception e) {
                        showToast("Không thể chọn ảnh: " + e.getMessage());
                    }
                } else {
                    showToast("Bạn chưa chọn ảnh.");
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_second, container, false);

        // Initialize views
        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnChangePassword = view.findViewById(R.id.btnChangepass);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        ivProfilePicture = view.findViewById(R.id.ivAvatar);

        db = FirebaseFirestore.getInstance();

        // Load username from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        currentUsername = prefs.getString("username", null);

        if (currentUsername != null) {
            loadUserDataRealtime(currentUsername);
        } else {
            Log.e(TAG, "Username not found in SharedPreferences.");
        }

        // Set click listeners
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        btnChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
            startActivity(intent);
        });
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditAccountActivity.class);
            startActivity(intent);
        });

        // Add click listener for avatar
        ivProfilePicture.setOnClickListener(v -> showAvatarOptionsDialog());

        return view;
    }

    private void showAvatarOptionsDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Tùy chọn ảnh đại diện")
                .setItems(new String[]{"Chọn ảnh mới", "Xem ảnh"}, (dialog, which) -> {
                    if (which == 0) {
                        checkAndRequestPermission();
                    } else if (which == 1) {
                        viewCurrentImage();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void viewCurrentImage() {
        if (encodedImage == null || encodedImage.isEmpty()) {
            showToast("Không có ảnh để xem.");
            return;
        }

        try {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bitmap != null) {
                // Create an ImageView for the dialog
                ImageView imageView = new ImageView(getContext());
                imageView.setImageBitmap(bitmap);
                imageView.setAdjustViewBounds(true);
                imageView.setPadding(16, 16, 16, 16);

                // Show the image in a dialog
                new AlertDialog.Builder(getContext())
                        .setTitle("Ảnh đại diện")
                        .setView(imageView)
                        .setPositiveButton("Đóng", null)
                        .show();
            } else {
                showToast("Lỗi tải ảnh.");
            }
        } catch (Exception e) {
            showToast("Lỗi xem ảnh: " + e.getMessage());
        }
    }

    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(
                requireContext().getContentResolver(),
                bitmap,
                "ProfileImage_" + System.currentTimeMillis(),
                null
        );
        return Uri.parse(path);
    }

    private void checkAndRequestPermission() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pickImageLauncher.launch(intent);
    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = (int) (bitmap.getHeight() * ((float) previewWidth / bitmap.getWidth()));
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private void updateUserImage(String encodedImage) {
        if (currentUsername == null || currentUsername.isEmpty()) {
            showToast("Không tìm thấy User ID.");
            return;
        }
        this.encodedImage = encodedImage; // Update the local encodedImage
        HashMap<String, Object> updates = new HashMap<>();
        updates.put("profileImage", encodedImage);

        db.collection("users").document(currentUsername)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showToast("Cập nhật ảnh thành công!");
                })
                .addOnFailureListener(e -> {
                    showToast("Lỗi cập nhật ảnh: " + e.getMessage());
                    Log.e(TAG, "Error updating profile image", e);
                });
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
                        String profileImage = documentSnapshot.getString("profileImage");

                        // Update UI
                        tvUserName.setText(name);
                        tvUserEmail.setText(email);

                        // Load profile picture
                        if (profileImage != null && !profileImage.isEmpty()) {
                            try {
                                encodedImage = profileImage; // Store encoded image
                                byte[] bytes = Base64.decode(profileImage, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                if (bitmap != null) {
                                    ivProfilePicture.setImageBitmap(bitmap);
                                } else {
                                    ivProfilePicture.setImageResource(R.drawable.ic_profilepic);
                                }
                            } catch (IllegalArgumentException ex) {
                                ivProfilePicture.setImageResource(R.drawable.ic_profilepic);
                                showToast("Lỗi hiển thị hình ảnh");
                            }
                        } else {
                            ivProfilePicture.setImageResource(R.drawable.ic_profilepic);
                            encodedImage = null;
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

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) {
            userListener.remove();
        }
    }
}
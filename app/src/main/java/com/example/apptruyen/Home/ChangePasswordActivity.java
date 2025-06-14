package com.example.apptruyen.Home;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.apptruyen.R;
import com.example.apptruyen.util.Utility;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.regex.Pattern;

public class ChangePasswordActivity extends AppCompatActivity {

    private static final String TAG = "ChangePasswordActivity";
    private static final String USER_PREFS = "user_prefs";
    private static final String USERNAME_KEY = "username";

    // UI Components
    private Toolbar toolbar;
    private TextInputEditText editOldPassword, editNewPassword, editConfirmPassword;
    private MaterialButton btnSavePassword;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseFirestore db;
    private String currentUsername;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        initViews();
        setupToolbar();
        initializeData();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        editOldPassword = findViewById(R.id.editOldPassword);
        editNewPassword = findViewById(R.id.editNewPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        btnSavePassword = findViewById(R.id.btnSavePassword);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initializeData() {
        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences(USER_PREFS, MODE_PRIVATE);
        currentUsername = prefs.getString(USERNAME_KEY, null);

        if (currentUsername == null) {
            showToast("Không xác định được tài khoản!");
            finish();
        }
    }

    private void setupListeners() {
        btnSavePassword.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String oldPassword = editOldPassword.getText().toString().trim();
        String newPassword = editNewPassword.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();

        if (!validateInput(oldPassword, newPassword, confirmPassword)) return;

        showLoading(true);
        db.collection("users").document(currentUsername).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String storedPassword = documentSnapshot.getString("password");
                        // Kiểm tra mật khẩu cũ (giả sử Utility có hàm verifyPassword)
                        if (storedPassword == null || !verifyPassword(oldPassword, storedPassword)) {
                            showLoading(false);
                            showToast("Mật khẩu cũ không đúng!");
                            return;
                        }
                        String hashedNewPassword = Utility.hashPassword(newPassword);
                        db.collection("users").document(currentUsername)
                                .update("password", hashedNewPassword)
                                .addOnSuccessListener(aVoid -> {
                                    showLoading(false);
                                    showToast("Đổi mật khẩu thành công!");
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    showLoading(false);
                                    Log.e(TAG, "Lỗi khi cập nhật mật khẩu: ", e);
                                    showToast("Lỗi: " + e.getMessage());
                                });
                    } else {
                        showLoading(false);
                        showToast("Không tìm thấy tài khoản!");
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Lỗi khi kiểm tra mật khẩu: ", e);
                    showToast("Lỗi: " + e.getMessage());
                });
    }

    private boolean verifyPassword(String plainPassword, String hashedPassword) {
        // Giả sử Utility có hàm verifyPassword cho bcrypt
        // Nếu không, dùng thư viện như jBCrypt
        try {
            return org.mindrot.jbcrypt.BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi kiểm tra mật khẩu: ", e);
            return false;
        }
    }

    private boolean validateInput(String oldPassword, String newPassword, String confirmPassword) {
        if (oldPassword.isEmpty()) {
            editOldPassword.setError("Vui lòng nhập mật khẩu cũ!");
            return false;
        }
        if (newPassword.isEmpty()) {
            editNewPassword.setError("Vui lòng nhập mật khẩu mới!");
            return false;
        }
        if (newPassword.length() < 6 || !Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{6,}$").matcher(newPassword).matches()) {
            editNewPassword.setError(newPassword.length() < 6 ? "Mật khẩu mới phải có ít nhất 6 ký tự!" :
                    "Mật khẩu cần chữ số, chữ thường, chữ hoa, ký tự đặc biệt!");
            return false;
        }
        if (!newPassword.equals(confirmPassword)) {
            editConfirmPassword.setError("Mật khẩu xác nhận không khớp!");
            return false;
        }
        return true;
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSavePassword.setEnabled(!show);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
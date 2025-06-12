package com.example.apptruyen.Home;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.apptruyen.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class EditAccountActivity extends AppCompatActivity {

    private EditText editUsername, editEmail, editPassword;
    private Button btnSave;
    private FirebaseFirestore db;
    private String username; // document ID trong Firestore

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_account);

        editUsername = findViewById(R.id.edit_username);
        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        btnSave = findViewById(R.id.btn_save);

        db = FirebaseFirestore.getInstance();

        // Lấy username từ SharedPreferences
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        username = prefs.getString("username", null);

        // Nếu username null, không thể tiếp tục
        if (username == null) {
            Toast.makeText(this, "Không xác định được tài khoản!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Tải thông tin người dùng từ Firestore
        loadUserData(username);

        // Bắt sự kiện lưu
        btnSave.setOnClickListener(v -> saveChanges(username));
    }

    private void loadUserData(String username) {
        DocumentReference docRef = db.collection("users").document(username);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("username");
                String email = documentSnapshot.getString("email");
                String password = documentSnapshot.getString("password");

                editUsername.setText(name);
                editEmail.setText(email);
                editPassword.setText(password);
            } else {
                Toast.makeText(this, "Không tìm thấy tài khoản", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Log.e("EditAccountActivity", "Lỗi khi tải dữ liệu: ", e);
            Toast.makeText(this, "Lỗi khi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void saveChanges(String oldUsername) {
        if (oldUsername == null || oldUsername.isEmpty()) {
            Toast.makeText(this, "Tài khoản không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        String newUsername = editUsername.getText().toString().trim();
        String newEmail = editEmail.getText().toString().trim();
        String newPassword = editPassword.getText().toString().trim();

        // Kiểm tra dữ liệu đầu vào
        if (newUsername.isEmpty() || newEmail.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            Toast.makeText(this, "Email không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newUsername.contains("/") || newUsername.contains(".") || newUsername.contains("#") ||
                newUsername.contains("$") || newUsername.contains("[") || newUsername.contains("]")) {
            Toast.makeText(this, "Tên tài khoản chứa ký tự không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mã hóa mật khẩu
        String hashedPassword = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", newUsername);
        userMap.put("email", newEmail);
        userMap.put("password", hashedPassword);

        // Sử dụng giao dịch để đảm bảo đồng bộ
        db.runTransaction(transaction -> {
            if (!newUsername.equals(oldUsername)) {
                // Xóa tài liệu cũ và tạo tài liệu mới
                transaction.delete(db.collection("users").document(oldUsername));
                transaction.set(db.collection("users").document(newUsername), userMap);
            } else {
                // Cập nhật tài liệu hiện tại
                transaction.set(db.collection("users").document(oldUsername), userMap, SetOptions.merge());
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            // Cập nhật SharedPreferences
            getSharedPreferences("user_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("username", newUsername)
                    .apply();
            Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Log.e("EditAccountActivity", "Lỗi khi cập nhật: ", e);
            Toast.makeText(this, "Lỗi khi cập nhật: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}
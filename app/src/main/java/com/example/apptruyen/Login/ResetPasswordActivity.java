package com.example.apptruyen.Login;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.apptruyen.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText newPasswordEditText, confirmPasswordEditText;
    private Button resetButton;
    private FirebaseFirestore db;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        db = FirebaseFirestore.getInstance();
        token = getIntent().getStringExtra("token");

        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        resetButton = findViewById(R.id.resetButton);

        resetButton.setOnClickListener(v -> validateAndResetPassword());
    }

    private void validateAndResetPassword() {
        String newPassword = newPasswordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        verifyTokenAndResetPassword(newPassword);
    }

    private void verifyTokenAndResetPassword(String newPassword) {
        db.collection("passwordResetTokens")
                .whereEqualTo("token", token)
                .whereEqualTo("used", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot tokenDoc = task.getResult().getDocuments().get(0);
                        Date expiryDate = tokenDoc.getDate("expiryDate");
                        String userId = tokenDoc.getString("userId");

                        if (expiryDate != null && expiryDate.after(new Date())) {
                            // Token hợp lệ, cập nhật mật khẩu
                            updatePassword(userId, newPassword, tokenDoc.getId());
                        } else {
                            Toast.makeText(this, "Token đã hết hạn", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Token không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updatePassword(String userId, String newPassword, String tokenDocId) {
        // Hash mật khẩu mới trước khi lưu (triển khai hàm hashPassword của bạn)
        String hashedPassword = hashPassword(newPassword);

        // Cập nhật mật khẩu người dùng
        Map<String, Object> updates = new HashMap<>();
        updates.put("password", hashedPassword);

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Đánh dấu token đã sử dụng
                    db.collection("passwordResetTokens").document(tokenDocId)
                            .update("used", true);

                    Toast.makeText(this, "Đặt lại mật khẩu thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi đặt lại mật khẩu", Toast.LENGTH_SHORT).show();
                });
    }

    private String hashPassword(String password) {
        // Triển khai hàm băm mật khẩu (sử dụng BCrypt, PBKDF2, v.v.)
        // Ví dụ đơn giản (KHÔNG sử dụng trong production):
        return "hashed_" + password;
    }
}
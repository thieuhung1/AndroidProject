package com.example.apptruyen.Login;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.apptruyen.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final int TOKEN_EXPIRY_HOURS = 2; // Token hết hạn sau 2 giờ
    private EditText emailEditText;
    private Button sendButton;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        db = FirebaseFirestore.getInstance();
        emailEditText = findViewById(R.id.emailEditText);
        sendButton = findViewById(R.id.sendButton);
        progressBar = findViewById(R.id.progressBar);

        sendButton.setOnClickListener(v -> handlePasswordReset());
        findViewById(R.id.backToLoginTextView).setOnClickListener(v -> finish());
    }

    private void handlePasswordReset() {
        String email = emailEditText.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);

        // Kiểm tra email tồn tại
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String userId = task.getResult().getDocuments().get(0).getId();
                        createResetToken(userId, email);
                    } else {
                        showProgress(false);
                        Toast.makeText(this, "Email không tồn tại", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    showProgress(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createResetToken(String userId, String email) {
        // Tạo token ngẫu nhiên
        String token = UUID.randomUUID().toString();

        // Thời gian hết hạn (hiện tại + 2 giờ)
        Date expiryDate = new Date(System.currentTimeMillis() + (TOKEN_EXPIRY_HOURS * 60 * 60 * 1000));

        // Lưu token vào Firestore
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("userId", userId);
        tokenData.put("email", email);
        tokenData.put("token", token);
        tokenData.put("expiryDate", expiryDate);
        tokenData.put("used", false);

        db.collection("passwordResetTokens")
                .add(tokenData)
                .addOnSuccessListener(documentReference -> {
                    sendResetEmail(email, token);
                })
                .addOnFailureListener(e -> {
                    showProgress(false);
                    Toast.makeText(this, "Lỗi tạo token", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendResetEmail(String email, String token) {
        // Tạo link reset (giả lập)
        String resetLink = "https://yourapp.com/reset-password?token=" + token;

        // TODO: Thay bằng dịch vụ gửi email thật
        String emailContent = "Nhấn vào link sau để đặt lại mật khẩu:\n" + resetLink +
                "\nLink có hiệu lực trong 2 giờ";

        // Hiển thị thông báo (thay cho gửi email thật)
        Toast.makeText(this,
                "Đã gửi link reset đến " + email + "\n" + emailContent,
                Toast.LENGTH_LONG).show();

        showProgress(false);
        finish();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        sendButton.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
    }
}
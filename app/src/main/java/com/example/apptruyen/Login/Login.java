package com.example.apptruyen.Login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.apptruyen.Home.Home;
import com.example.apptruyen.R;
import com.example.apptruyen.register.register;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends AppCompatActivity {
    private EditText emailEditText, passwordEditText;
    private ImageView eyeToggle;
    private Button loginButton;
    private boolean passwordVisible;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        db = FirebaseFirestore.getInstance();

        eyeToggle.setOnClickListener(v -> togglePasswordVisibility());
        findViewById(R.id.signupButton).setOnClickListener(v -> startActivity(new Intent(this, register.class)));
        loginButton.setOnClickListener(v -> attemptLogin());
    }

    private void initViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        eyeToggle = findViewById(R.id.eyeopen);
        loginButton = findViewById(R.id.loginButton);
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        passwordEditText.setTransformationMethod(passwordVisible ?
                HideReturnsTransformationMethod.getInstance() :
                PasswordTransformationMethod.getInstance());
        eyeToggle.setImageResource(passwordVisible ? R.drawable.eye_close : R.drawable.eye_open);
        passwordEditText.setSelection(passwordEditText.length());
    }

    private void attemptLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showToast("Vui lòng nhập đầy đủ thông tin");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Email không hợp lệ");
            return;
        }

        setLoading(true);
        db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(docs -> {
                    setLoading(false);
                    if (!docs.isEmpty() && password.equals(docs.getDocuments().get(0).getString("password"))) {
                        // Lấy username từ document
                        String username = docs.getDocuments().get(0).getString("username");

                        // ✅ Lưu username vào SharedPreferences
                        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                        prefs.edit().putString("username", username).apply();

                        showToast("Đăng nhập thành công");

                        // Chuyển sang Home
                        startActivity(new Intent(this, Home.class));
                        finish();
                    } else {
                        showToast(docs.isEmpty() ? "Email chưa được đăng ký" : "Sai mật khẩu");
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showToast("Lỗi: " + e.getMessage());
                });
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        loginButton.setText(loading ? "Đang đăng nhập..." : "Đăng nhập");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

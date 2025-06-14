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
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.apptruyen.Home.Home;
import com.example.apptruyen.R;
import com.example.apptruyen.register.register;
import com.example.apptruyen.util.Utility;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends AppCompatActivity {
    private static final String TAG = "LoginActivity"; // Thẻ log để debug

    private EditText emailEditText, passwordEditText;
    private ImageView eyeToggle;
    private Button loginButton;
    private TextView forgotPasswordTextView;
    private boolean passwordVisible;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Kiểm tra đăng nhập tự động
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        if (prefs.getBoolean("is_logged_in", false)) {
            startActivity(new Intent(this, Home.class));
            finish();
            return;
        }

        initViews();
        db = FirebaseFirestore.getInstance();

        eyeToggle.setOnClickListener(v -> togglePasswordVisibility());
        findViewById(R.id.signupButton).setOnClickListener(v ->
                startActivity(new Intent(this, register.class)));
        loginButton.setOnClickListener(v -> attemptLogin());

        // Thêm xử lý click cho "Quên mật khẩu"
        forgotPasswordTextView.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }

    // Khởi tạo view
    private void initViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        eyeToggle = findViewById(R.id.eyeopen);
        loginButton = findViewById(R.id.loginButton);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);
    }

    // Chuyển đổi hiển thị mật khẩu
    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        passwordEditText.setTransformationMethod(passwordVisible ?
                HideReturnsTransformationMethod.getInstance() :
                PasswordTransformationMethod.getInstance());
        eyeToggle.setImageResource(passwordVisible ? R.drawable.eye_close : R.drawable.eye_open);
        passwordEditText.setSelection(passwordEditText.length());
    }

    // Thử đăng nhập
    private void attemptLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (!validateInputs(email, password)) return;

        setLoading(true);
        db.collection("users").whereEqualTo("email", email).limit(1).get()
                .addOnSuccessListener(docs -> {
                    setLoading(false);
                    if (!docs.isEmpty()) {
                        String hashedPasswordFromDb = docs.getDocuments().get(0).getString("password");
                        String username = docs.getDocuments().get(0).getString("username");

                        if (hashedPasswordFromDb == null || username == null) {
                            Utility.showToast(this, "Lỗi dữ liệu người dùng");
                            return;
                        }

                        if (Utility.checkPassword(password, hashedPasswordFromDb)) {
                            // Lưu trạng thái đăng nhập
                            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                            prefs.edit()
                                    .putString("username", username)
                                    .putBoolean("is_logged_in", true)
                                    .apply();

                            Utility.showToast(this, "Đăng nhập thành công");
                            startActivity(new Intent(this, Home.class));
                            finish();
                        } else {
                            Utility.showToast(this, "Sai mật khẩu");
                        }
                    } else {
                        Utility.showToast(this, "Email chưa được đăng ký");
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Utility.showToast(this, "Lỗi: " + e.getMessage());
                });
    }

    // Kiểm tra đầu vào
    private boolean validateInputs(String email, String password) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Utility.showToast(this, "Vui lòng nhập đầy đủ thông tin");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Utility.showToast(this, "Email không hợp lệ");
            return false;
        }
        return true;
    }

    // Cài đặt trạng thái tải
    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        loginButton.setText(loading ? "Đang đăng nhập..." : "Đăng nhập");
    }
}
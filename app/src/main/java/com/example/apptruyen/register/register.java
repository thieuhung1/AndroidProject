package com.example.apptruyen.register;

import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.apptruyen.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class register extends AppCompatActivity {

    EditText usernameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    Button signupbt1;
    TextView Loginview1;
    ImageView eyeToggle;
    FirebaseFirestore firestore;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        /// Ánh xạ các view
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        emailEditText = findViewById(R.id.emailEditText);
        signupbt1 = findViewById(R.id.signupButton);
        Loginview1 = findViewById(R.id.Loginview1);
        eyeToggle = findViewById(R.id.passwordToggle); // Đảm bảo ID đúng trong XML

        firestore = FirebaseFirestore.getInstance();

        /// Xử lý ẩn/hiện mật khẩu
        eyeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                passwordVisible = !passwordVisible;
                if (passwordVisible) {
                    passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    confirmPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    eyeToggle.setImageResource(R.drawable.eye_close); // icon mắt đóng
                } else {
                    passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    confirmPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    eyeToggle.setImageResource(R.drawable.eye_open); // icon mắt mở
                }
                passwordEditText.setSelection(passwordEditText.getText().length());
                confirmPasswordEditText.setSelection(confirmPasswordEditText.getText().length());
            }
        });

        /// Xử lý lề hệ thống (đối với edge-to-edge layout)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signup_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        /// Xử lý nút Đăng ký
        signupbt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString().trim();
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();
                String confirm = confirmPasswordEditText.getText().toString().trim();

                // Kiểm tra dữ liệu đầu vào
                if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                    Toast.makeText(register.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(register.this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(confirm)) {
                    Toast.makeText(register.this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Tạo Map user để lưu vào Firestore
                Map<String, Object> user = new HashMap<>();
                user.put("username", username);
                user.put("email", email);
                user.put("password", password); // Gợi ý: Nên mã hóa mật khẩu trong sản phẩm thật

                firestore.collection("users")
                        .document(username)
                        .set(user)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(register.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                            finish(); // Quay lại màn hình đăng nhập
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(register.this, "Đăng ký thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });

        /// Xử lý "Đã có tài khoản? Đăng nhập"
        Loginview1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Đóng activity này, quay lại Login
            }
        });
    }
}

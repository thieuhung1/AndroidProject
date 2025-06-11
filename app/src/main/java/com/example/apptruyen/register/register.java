package com.example.apptruyen.register;

import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
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
    private EditText usernameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private ImageView eyeToggle;
    private boolean passwordVisible;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        db = FirebaseFirestore.getInstance();

        eyeToggle.setOnClickListener(v -> togglePasswordVisibility());
        findViewById(R.id.signupButton).setOnClickListener(v -> registerUser());
        findViewById(R.id.Loginview1).setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signup_main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private void initViews() {
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        eyeToggle = findViewById(R.id.passwordToggle);
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        int inputType = passwordVisible ?
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;

        passwordEditText.setInputType(inputType);
        confirmPasswordEditText.setInputType(inputType);
        eyeToggle.setImageResource(passwordVisible ? R.drawable.eye_close : R.drawable.eye_open);

        passwordEditText.setSelection(passwordEditText.length());
        confirmPasswordEditText.setSelection(confirmPasswordEditText.length());
    }

    private void registerUser() {
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirm = confirmPasswordEditText.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showToast("Vui lòng nhập đầy đủ thông tin");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Email không hợp lệ");
            return;
        }

        if (!password.equals(confirm)) {
            showToast("Mật khẩu xác nhận không khớp");
            return;
        }

        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("email", email);
        user.put("password", password); // TODO: Hash password before storing

        db.collection("users").document(username).set(user)
                .addOnSuccessListener(a -> {
                    showToast("Đăng ký thành công!");
                    finish();
                })
                .addOnFailureListener(e -> showToast("Đăng ký thất bại: " + e.getMessage()));
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
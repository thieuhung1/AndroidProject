package com.example.apptruyen.register;

import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.apptruyen.R;
import com.example.apptruyen.util.Utility;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

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
        setInputTypeForEditText(passwordEditText, inputType);
        setInputTypeForEditText(confirmPasswordEditText, inputType);
        eyeToggle.setImageResource(passwordVisible ? R.drawable.eye_close : R.drawable.eye_open);
    }

    private void setInputTypeForEditText(EditText editText, int inputType) {
        editText.setInputType(inputType);
        editText.setSelection(editText.length());
    }

    private void registerUser() {
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirm = confirmPasswordEditText.getText().toString().trim();

        if (!validateInputs(username, email, password, confirm)) {
            return;
        }

        db.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener(docs -> {
                    if (!docs.isEmpty()) {
                        Utility.showToast(this, "Email đã được sử dụng");
                        return;
                    }
                    String hashedPassword = Utility.hashPassword(password);
                    Map<String, Object> user = new HashMap<>();
                    user.put("username", username);
                    user.put("email", email);
                    user.put("password", hashedPassword);
                    db.collection("users").document(username).set(user)
                            .addOnSuccessListener(a -> {
                                Utility.showToast(this, "Đăng ký thành công!");
                                finish();
                            })
                            .addOnFailureListener(e -> Utility.showToast(this, "Đăng ký thất bại: " + e.getMessage()));
                })
                .addOnFailureListener(e -> Utility.showToast(this, "Lỗi kiểm tra email: " + e.getMessage()));
    }

    private boolean validateInputs(String username, String email, String password, String confirm) {
        if (username.isEmpty()) {
            Utility.showToast(this, "Vui lòng nhập tên đăng nhập");
            return false;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Utility.showToast(this, email.isEmpty() ? "Vui lòng nhập email" : "Email không hợp lệ");
            return false;
        }
        if (password.isEmpty() || password.length() < 6 || !Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{6,}$").matcher(password).matches()) {
            if (password.isEmpty()) Utility.showToast(this, "Vui lòng nhập mật khẩu");
            else if (password.length() < 6) Utility.showToast(this, "Mật khẩu phải có ít nhất 6 ký tự");
            else Utility.showToast(this, "Mật khẩu cần chữ số, chữ thường, chữ hoa, ký tự đặc biệt");
            return false;
        }
        if (confirm.isEmpty() || !password.equals(confirm)) {
            Utility.showToast(this, confirm.isEmpty() ? "Vui lòng xác nhận mật khẩu" : "Mật khẩu xác nhận không khớp");
            return false;
        }
        return true;
    }
}
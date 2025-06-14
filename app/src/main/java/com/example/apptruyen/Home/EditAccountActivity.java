package com.example.apptruyen.Home;

import android.Manifest;
import android.app.DatePickerDialog;
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
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.apptruyen.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditAccountActivity extends AppCompatActivity {

    private static final String TAG = "EditAccountActivity";
    private static final String USER_PREFS = "user_prefs";
    private static final String USERNAME_KEY = "username";

    private TextInputEditText editUsername, editEmail, editDob, editAddInterest;
    private CircleImageView ivProfileImage;
    private ChipGroup cgGender, cgInterests;
    private MaterialButton btnSave, btnAddInterest;
    private ProgressBar progressBar;

    private String currentUsername;
    private String encodedImage;
    private List<String> interestsList = new ArrayList<>();
    private FirebaseFirestore db;

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
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        ivProfileImage.setImageBitmap(bitmap);
                        encodedImage = encodeImage(bitmap);
                        if (inputStream != null) inputStream.close();
                    } catch (Exception e) {
                        showToast("Không thể chọn ảnh: " + e.getMessage());
                    }
                } else {
                    showToast("Bạn chưa chọn ảnh.");
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_account);

        // Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();

        // Ánh xạ view
        initViews();

        // Load thông tin người dùng
        loadUsername();
        loadUserData();

        // Thiết lập sự kiện
        setupListeners();
    }

    private void initViews() {
        editUsername = findViewById(R.id.edit_username);
        editEmail = findViewById(R.id.edit_email);
        editDob = findViewById(R.id.edit_dob);
        editAddInterest = findViewById(R.id.edit_add_interest);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        cgGender = findViewById(R.id.cgGender);
        cgInterests = findViewById(R.id.cgInterests);
        btnSave = findViewById(R.id.btn_save);
        btnAddInterest = findViewById(R.id.btnAddInterest);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadUsername() {
        SharedPreferences prefs = getSharedPreferences(USER_PREFS, MODE_PRIVATE);
        currentUsername = prefs.getString(USERNAME_KEY, null);
        if (currentUsername == null) {
            showToast("Không tìm thấy username");
            finish();
        }
    }

    private void setupListeners() {
        // Sự kiện click ảnh đại diện
        ivProfileImage.setOnClickListener(v -> checkAndRequestPermission());

        // Sự kiện chọn ngày sinh
        editDob.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                editDob.setText(String.format("%02d/%02d/%d", day, month + 1, year));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Sự kiện thêm sở thích
        btnAddInterest.setOnClickListener(v -> addInterest());

        // Sự kiện lưu thông tin
        btnSave.setOnClickListener(v -> saveUserData());
    }

    private void checkAndRequestPermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pickImageLauncher.launch(intent);
    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = (int) (bitmap.getHeight() * ((float) previewWidth / bitmap.getWidth()));
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, true);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private void saveUserData() {
        String username = editUsername.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String dob = editDob.getText().toString().trim();

        if (!validateInput(username, email)) {
            showLoading(false);
            return;
        }

        showLoading(true);

        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("email", email);
        data.put("dob", dob);
        data.put("gender", getSelectedGender());
        data.put("interests", interestsList);
        if (encodedImage != null) data.put("profileImage", encodedImage);
        data.put("updatedAt", System.currentTimeMillis());

        db.collection("users").document(currentUsername)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    updatePrefs(username);
                    showLoading(false);
                    showToast("Cập nhật thành công");
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("image_updated", true);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showToast("Lỗi lưu dữ liệu: " + e.getMessage());
                    Log.e(TAG, "Error saving user data", e);
                });
    }

    private boolean validateInput(String username, String email) {
        if (username.isEmpty()) {
            editUsername.setError("Vui lòng nhập tên người dùng!");
            return false;
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError(email.isEmpty() ? "Vui lòng nhập email!" : "Email không hợp lệ!");
            return false;
        }
        if (containsInvalidChars(username)) {
            editUsername.setError("Tên người dùng chứa ký tự không hợp lệ!");
            return false;
        }
        return true;
    }

    private boolean containsInvalidChars(String username) {
        for (String c : new String[]{"/", ".", "#", "$", "[", "]", " "}) {
            if (username.contains(c)) return true;
        }
        return false;
    }

    private void loadUserData() {
        showLoading(true);
        db.collection("users").document(currentUsername).get()
                .addOnSuccessListener(doc -> {
                    showLoading(false);
                    if (doc.exists()) {
                        editUsername.setText(doc.getString("username"));
                        editEmail.setText(doc.getString("email"));
                        editDob.setText(doc.getString("dob"));
                        selectGenderChip(doc.getString("gender"));

                        // Load sở thích
                        List<String> interests = (List<String>) doc.get("interests");
                        if (interests != null) {
                            interestsList.clear();
                            for (String interest : interests) {
                                interestsList.add(interest);
                                addInterestChip(interest);
                            }
                        }

                        // Load ảnh đại diện
                        String profileImage = doc.getString("profileImage");
                        if (profileImage != null && !profileImage.isEmpty()) {
                            try {
                                byte[] bytes = Base64.decode(profileImage, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                if (bitmap != null) {
                                    ivProfileImage.setImageBitmap(bitmap);
                                } else {
                                    ivProfileImage.setImageResource(R.drawable.ic_profilepic);
                                }
                            } catch (IllegalArgumentException e) {
                                ivProfileImage.setImageResource(R.drawable.ic_profilepic);
                                showToast("Lỗi hiển thị hình ảnh");
                            }
                        } else {
                            ivProfileImage.setImageResource(R.drawable.ic_profilepic);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showToast("Lỗi tải dữ liệu: " + e.getMessage());
                    Log.e(TAG, "Error loading user data", e);
                });
    }

    private void addInterest() {
        String interest = editAddInterest.getText().toString().trim();
        if (!interest.isEmpty() && !interestsList.contains(interest)) {
            interestsList.add(interest);
            addInterestChip(interest);
            editAddInterest.setText("");
        }
    }

    private void addInterestChip(String interest) {
        Chip chip = new Chip(this);
        chip.setText(interest);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            cgInterests.removeView(chip);
            interestsList.remove(interest);
        });
        cgInterests.addView(chip);
    }

    private void selectGenderChip(String gender) {
        int id = gender == null ? -1 :
                gender.equalsIgnoreCase("Nam") ? R.id.chipMale :
                        gender.equalsIgnoreCase("Nữ") ? R.id.chipFemale : R.id.chipOther;
        if (id != -1) cgGender.check(id);
    }

    private String getSelectedGender() {
        int id = cgGender.getCheckedChipId();
        if (id == R.id.chipMale) return "Nam";
        if (id == R.id.chipFemale) return "Nữ";
        return "Khác";
    }

    private void updatePrefs(String username) {
        getSharedPreferences(USER_PREFS, MODE_PRIVATE)
                .edit()
                .putString(USERNAME_KEY, username)
                .apply();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
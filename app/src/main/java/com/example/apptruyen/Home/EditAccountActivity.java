package com.example.apptruyen.Home;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.apptruyen.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EditAccountActivity extends AppCompatActivity {

    private static final String TAG = "EditAccountActivity";
    private static final String USER_PREFS = "user_prefs";
    private static final String USERNAME_KEY = "username";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int STORAGE_PERMISSION_CODE = 1;

    // UI Components
    private Toolbar toolbar;
    private ImageView profileImage;
    private TextInputEditText editUsername, editEmail, editDob, editAddInterest;
    private ChipGroup cgGender, cgInterests;
    private MaterialButton btnSave, btnAddInterest;
    private ProgressBar progressBar;

    // Data
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String currentUsername;
    private List<String> interestsList = new ArrayList<>();
    private Uri imageUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_account);
        initViews();
        setupToolbar();
        initializeData();
        setupListeners();
        loadUserData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        profileImage = findViewById(R.id.ivProfileImage);
        editUsername = findViewById(R.id.edit_username);
        editEmail = findViewById(R.id.edit_email);
        editDob = findViewById(R.id.edit_dob);
        editAddInterest = findViewById(R.id.edit_add_interest);
        cgGender = findViewById(R.id.cgGender);
        cgInterests = findViewById(R.id.cgInterests);
        btnSave = findViewById(R.id.btn_save);
        btnAddInterest = findViewById(R.id.btnAddInterest);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initializeData() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        SharedPreferences prefs = getSharedPreferences(USER_PREFS, MODE_PRIVATE);
        currentUsername = prefs.getString(USERNAME_KEY, null);

        if (currentUsername == null) {
            showToast("Không xác định được tài khoản!");
            finish();
        }
    }

    private void setupListeners() {
        editDob.setOnClickListener(v -> showDatePicker());
        btnAddInterest.setOnClickListener(v -> addInterest());
        btnSave.setOnClickListener(v -> saveChanges());
        editAddInterest.setOnEditorActionListener((v, actionId, event) -> {
            addInterest();
            return true;
        });
        profileImage.setOnClickListener(v -> requestStoragePermission());
    }

    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        } else {
            openFileChooser();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFileChooser();
            } else {
                showToast("Quyền truy cập bộ nhớ bị từ chối!");
            }
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImage.setImageURI(imageUri);
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = String.format("%02d/%02d/%d", day, month + 1, year);
            editDob.setText(date);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void addInterest() {
        String interest = editAddInterest.getText().toString().trim();
        if (interest.isEmpty() || interestsList.contains(interest)) {
            showToast(interest.isEmpty() ? "Vui lòng nhập sở thích!" : "Sở thích đã tồn tại!");
            return;
        }
        interestsList.add(interest);
        addInterestChip(interest);
        editAddInterest.setText("");
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

    private void loadUserData() {
        showLoading(true);
        db.collection("users").document(currentUsername).get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);
                    if (documentSnapshot.exists()) {
                        populateUserData(documentSnapshot.getData());
                    } else {
                        showToast("Không tìm thấy tài khoản");
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Lỗi khi tải dữ liệu: ", e);
                    showToast("Lỗi: " + e.getMessage());
                });
    }

    private void populateUserData(Map<String, Object> userData) {
        if (userData == null) return;
        editUsername.setText(getStringValue(userData, "username"));
        editEmail.setText(getStringValue(userData, "email"));
        editDob.setText(getStringValue(userData, "dob"));
        selectGenderChip(getStringValue(userData, "gender"));
        Object interests = userData.get("interests");
        if (interests instanceof List) {
            loadInterests((List<String>) interests);
        }
        String profileImageUrl = getStringValue(userData, "profileImage");
        if (!profileImageUrl.isEmpty()) {
            Picasso.get().load(profileImageUrl).into(profileImage);
        }
    }

    private void loadInterests(List<String> interests) {
        if (interests != null) {
            interestsList.clear();
            cgInterests.removeAllViews();
            for (String interest : interests) {
                interestsList.add(interest);
                addInterestChip(interest);
            }
        }
    }

    private String getStringValue(Map<String, Object> map, String key) {
        return map.get(key) != null ? map.get(key).toString() : "";
    }

    private void selectGenderChip(String gender) {
        if (gender != null && !gender.isEmpty()) {
            int chipId = gender.equalsIgnoreCase("Nam") ? R.id.chipMale :
                    gender.equalsIgnoreCase("Nữ") ? R.id.chipFemale : R.id.chipOther;
            View chip = findViewById(chipId);
            if (chip != null) chip.performClick();
        }
    }

    private void saveChanges() {
        if (!validateInput()) return;

        showLoading(true);
        if (imageUri != null) {
            uploadImageToStorage();
        } else {
            saveToFirestore("");
        }
    }

    private void uploadImageToStorage() {
        StorageReference storageRef = storage.getReference().child("profile_images/" + UUID.randomUUID().toString());
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> saveToFirestore(uri.toString()))
                        .addOnFailureListener(e -> {
                            showLoading(false);
                            showToast("Lỗi tải URL: " + e.getMessage());
                        }))
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showToast("Lỗi tải ảnh: " + e.getMessage());
                });
    }

    private void saveToFirestore(String profileImageUrl) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", editUsername.getText().toString().trim());
        userMap.put("email", editEmail.getText().toString().trim());
        userMap.put("dob", editDob.getText().toString().trim());
        userMap.put("gender", getSelectedGender());
        userMap.put("interests", interestsList);
        if (!profileImageUrl.isEmpty()) {
            userMap.put("profileImage", profileImageUrl);
        }
        userMap.put("updatedAt", System.currentTimeMillis());

        db.runTransaction(transaction -> {
            if (!userMap.get("username").equals(currentUsername) &&
                    transaction.get(db.collection("users").document(userMap.get("username").toString())).exists()) {
                throw new RuntimeException("Tên người dùng đã tồn tại!");
            }
            transaction.set(db.collection("users").document(userMap.get("username").toString()), userMap, SetOptions.merge());
            if (!currentUsername.equals(userMap.get("username"))) {
                transaction.delete(db.collection("users").document(currentUsername));
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            showLoading(false);
            updateSharedPreferences(userMap.get("username").toString());
            showToast("Cập nhật thành công!");
            finish();
        }).addOnFailureListener(e -> {
            showLoading(false);
            Log.e(TAG, "Lỗi khi cập nhật: ", e);
            showToast("Lỗi: " + e.getMessage());
        });
    }

    private boolean validateInput() {
        String username = editUsername.getText().toString().trim();
        String email = editEmail.getText().toString().trim();

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
        for (String invalidChar : new String[]{"/", ".", "#", "$", "[", "]", " "}) {
            if (username.contains(invalidChar)) return true;
        }
        return false;
    }

    private String getSelectedGender() {
        int selectedId = cgGender.getCheckedChipId();
        return selectedId == R.id.chipMale ? "Nam" : selectedId == R.id.chipFemale ? "Nữ" : "Khác";
    }

    private void updateSharedPreferences(String newUsername) {
        getSharedPreferences(USER_PREFS, MODE_PRIVATE).edit().putString(USERNAME_KEY, newUsername).apply();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
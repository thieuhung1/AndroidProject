package com.example.apptruyen.model;

import java.util.List;

public class User {
    private String username;
    private String email;
    private String password;
    private String role; // Vai trò của người dùng (user hoặc admin)
    private String Img;

    // Thêm các thuộc tính mới cho EditAccountActivity
    private String dob; // Ngày sinh
    private String gender; // Giới tính
    private List<String> interests; // Danh sách sở thích
    private long createdAt; // Thời gian tạo tài khoản
    private long updatedAt; // Thời gian cập nhật cuối

    // Constructor gốc - giữ nguyên
    public User(String username, String email, String password, String role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.Img = Img;
    }

    // Constructor mở rộng với đầy đủ thông tin
    public User(String username, String email, String password, String role,
                String img, String dob, String gender, List<String> interests) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.Img = img;
        this.dob = dob;
        this.gender = gender;
        this.interests = interests;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Constructor mặc định cho Firestore
    public User() {
        // Required for Firestore deserialization
    }

    // Getter và Setter gốc - giữ nguyên
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getImg() {
        return Img;
    }

    public void setImg(String img) {
        this.Img = img;
    }

    // Getter và Setter cho các thuộc tính mới
    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public List<String> getInterests() {
        return interests;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Phương thức tiện ích
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    public boolean hasProfileImage() {
        return Img != null && !Img.isEmpty();
    }

    public boolean hasInterests() {
        return interests != null && !interests.isEmpty();
    }

    public void addInterest(String interest) {
        if (interests != null && !interests.contains(interest)) {
            interests.add(interest);
        }
    }

    public void removeInterest(String interest) {
        if (interests != null) {
            interests.remove(interest);
        }
    }

    public boolean hasCompleteProfile() {
        return username != null && !username.isEmpty() &&
                email != null && !email.isEmpty() &&
                dob != null && !dob.isEmpty() &&
                gender != null && !gender.isEmpty();
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", dob='" + dob + '\'' +
                ", gender='" + gender + '\'' +
                ", interests=" + interests +
                ", hasImage=" + hasProfileImage() +
                '}';
    }
}
package com.chaintrust.model;

public class AuthResponse {

    private boolean success;
    private String message;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String metaMaskAddress;
    private boolean newlyRegistered;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMetaMaskAddress() {
        return metaMaskAddress;
    }

    public void setMetaMaskAddress(String metaMaskAddress) {
        this.metaMaskAddress = metaMaskAddress;
    }

    public boolean isNewlyRegistered() {
        return newlyRegistered;
    }

    public void setNewlyRegistered(boolean newlyRegistered) {
        this.newlyRegistered = newlyRegistered;
    }
}

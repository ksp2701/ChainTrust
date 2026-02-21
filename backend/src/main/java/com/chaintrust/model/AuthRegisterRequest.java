package com.chaintrust.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AuthRegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 120, message = "Full name is too long")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    @Size(max = 320, message = "Email is too long")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^\\+?[0-9\\s\\-()]{7,25}$",
            message = "Phone can include digits, spaces, (), and - with an optional leading +"
    )
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
    private String password;

    @Pattern(
            regexp = "^$|^0x[a-fA-F0-9]{40}$",
            message = "MetaMask address must be a valid EVM address"
    )
    private String metaMaskAddress;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMetaMaskAddress() {
        return metaMaskAddress;
    }

    public void setMetaMaskAddress(String metaMaskAddress) {
        this.metaMaskAddress = metaMaskAddress;
    }
}

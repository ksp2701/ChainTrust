package com.chaintrust.service;

import com.chaintrust.model.AuthLoginRequest;
import com.chaintrust.model.AuthRegisterRequest;
import com.chaintrust.model.AuthResponse;
import com.chaintrust.model.UserAccountEntity;
import com.chaintrust.model.UserWalletEntity;
import com.chaintrust.repository.UserAccountRepository;
import com.chaintrust.repository.UserWalletRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final UserWalletRepository userWalletRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    public AuthService(UserAccountRepository userAccountRepository, UserWalletRepository userWalletRepository) {
        this.userAccountRepository = userAccountRepository;
        this.userWalletRepository = userWalletRepository;
    }

    @Transactional
    public AuthResponse register(AuthRegisterRequest request) {
        String email = requireTrimmed(request.getEmail(), "Email is required");
        String emailNormalized = normalizeEmail(email);
        String fullName = requireTrimmed(request.getFullName(), "Full name is required");
        String password = requireTrimmed(request.getPassword(), "Password is required");
        String phone = requireTrimmed(request.getPhone(), "Phone number is required");
        String phoneNormalized = normalizePhone(phone);
        if (phoneNormalized == null) {
            throw new IllegalArgumentException("Phone must be 7-20 digits and may start with +");
        }
        String walletAddress = optionalTrimmed(request.getMetaMaskAddress());
        String walletAddressNormalized = normalizeWallet(walletAddress);

        if (userAccountRepository.existsByEmailNormalized(emailNormalized)) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (userAccountRepository.existsByPhoneNormalized(phoneNormalized)) {
            throw new IllegalArgumentException("Phone number is already registered");
        }
        if (walletAddressNormalized != null && userWalletRepository.existsByWalletAddressNormalized(walletAddressNormalized)) {
            throw new IllegalArgumentException("MetaMask wallet is already linked to another account");
        }

        UserAccountEntity user = new UserAccountEntity();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setEmailNormalized(emailNormalized);
        user.setPhone(phone);
        user.setPhoneNormalized(phoneNormalized);
        user.setPasswordHash(passwordEncoder.encode(password));
        user = userAccountRepository.save(user);

        UserWalletEntity wallet = null;
        if (walletAddress != null) {
            wallet = new UserWalletEntity();
            wallet.setUser(user);
            wallet.setWalletAddress(walletAddress);
            wallet.setWalletAddressNormalized(walletAddressNormalized);
            wallet.setPrimaryWallet(true);
            wallet.setVerifiedAt(null);
            wallet = userWalletRepository.save(wallet);
        }

        return toResponse(user, wallet, "Registered successfully", true);
    }

    @Transactional
    public AuthResponse login(AuthLoginRequest request) {
        String email = requireTrimmed(request.getEmail(), "Email is required");
        String password = requireTrimmed(request.getPassword(), "Password is required");
        String emailNormalized = normalizeEmail(email);

        Optional<UserAccountEntity> optional = userAccountRepository.findByEmailNormalized(emailNormalized);
        if (optional.isEmpty()) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        UserAccountEntity user = optional.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        user.setLastLoginAt(Instant.now());
        user = userAccountRepository.save(user);

        UserWalletEntity wallet = userWalletRepository
                .findFirstByUser_IdOrderByPrimaryWalletDescIdAsc(user.getId())
                .orElse(null);

        return toResponse(user, wallet, "Login successful", false);
    }

    private AuthResponse toResponse(
            UserAccountEntity user,
            UserWalletEntity wallet,
            String message,
            boolean newlyRegistered) {
        AuthResponse response = new AuthResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setUserId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setMetaMaskAddress(wallet != null ? wallet.getWalletAddress() : null);
        response.setNewlyRegistered(newlyRegistered);
        return response;
    }

    private static String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    private static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String compact = phone.replaceAll("[\\s\\-()]", "");
        if (compact.isBlank()) {
            return null;
        }
        boolean hasLeadingPlus = compact.startsWith("+");
        String digits = hasLeadingPlus ? compact.substring(1) : compact;
        if (!digits.matches("^[0-9]{7,20}$")) {
            return null;
        }
        return hasLeadingPlus ? "+" + digits : digits;
    }

    private static String normalizeWallet(String walletAddress) {
        if (walletAddress == null || walletAddress.isBlank()) {
            return null;
        }
        return walletAddress.toLowerCase(Locale.ROOT);
    }

    private static String requireTrimmed(String value, String errorMessage) {
        String out = optionalTrimmed(value);
        if (out == null) {
            throw new IllegalArgumentException(errorMessage);
        }
        return out;
    }

    private static String optionalTrimmed(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

package com.chaintrust.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "user_wallets",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_wallet_pair", columnNames = {"user_id", "wallet_address_normalized"})
        },
        indexes = {
                @Index(name = "idx_user_wallet_addr_norm", columnList = "wallet_address_normalized", unique = true),
                @Index(name = "idx_user_wallet_user", columnList = "user_id"),
                @Index(name = "idx_user_wallet_primary", columnList = "is_primary")
        }
)
public class UserWalletEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_wallet_user"))
    private UserAccountEntity user;

    @Column(name = "wallet_address", nullable = false, length = 42)
    private String walletAddress;

    @Column(name = "wallet_address_normalized", nullable = false, length = 42, unique = true)
    private String walletAddressNormalized;

    @Column(name = "is_primary", nullable = false)
    private boolean primaryWallet;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public UserAccountEntity getUser() {
        return user;
    }

    public void setUser(UserAccountEntity user) {
        this.user = user;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public String getWalletAddressNormalized() {
        return walletAddressNormalized;
    }

    public void setWalletAddressNormalized(String walletAddressNormalized) {
        this.walletAddressNormalized = walletAddressNormalized;
    }

    public boolean isPrimaryWallet() {
        return primaryWallet;
    }

    public void setPrimaryWallet(boolean primaryWallet) {
        this.primaryWallet = primaryWallet;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}

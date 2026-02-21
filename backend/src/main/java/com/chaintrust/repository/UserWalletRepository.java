package com.chaintrust.repository;

import com.chaintrust.model.UserWalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserWalletRepository extends JpaRepository<UserWalletEntity, Long> {

    boolean existsByWalletAddressNormalized(String walletAddressNormalized);

    Optional<UserWalletEntity> findFirstByUser_IdOrderByPrimaryWalletDescIdAsc(Long userId);
}

package com.chaintrust.repository;

import com.chaintrust.model.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long> {

    Optional<UserAccountEntity> findByEmailNormalized(String emailNormalized);

    boolean existsByEmailNormalized(String emailNormalized);

    boolean existsByPhoneNormalized(String phoneNormalized);
}

package com.chaintrust.repository;

import com.chaintrust.model.LoanDecisionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanDecisionRepository extends JpaRepository<LoanDecisionEntity, Long> {
    Optional<LoanDecisionEntity> findByDecisionHash(String decisionHash);
    List<LoanDecisionEntity> findByOutcomeLabelIn(List<String> outcomeLabels);
}

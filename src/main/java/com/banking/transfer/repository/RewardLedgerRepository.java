package com.banking.transfer.repository;

import com.banking.transfer.entity.RewardLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RewardLedgerRepository extends JpaRepository<RewardLedger, Long> {

    // Fast check to prevent unique constraint violation exceptions
    boolean existsByTransactionId(String transactionId);

    // Used as idempotency guard — never double-reward a transaction
    Optional<RewardLedger> findByTransactionId(String transactionId);

    // All reward entries for an account, newest first
    List<RewardLedger> findByAccountIdOrderByCreatedOnDesc(Long accountId);

    // Total points for an account
    @Query("SELECT COALESCE(SUM(r.pointsEarned), 0) FROM RewardLedger r WHERE r.accountId = ?1")
    int sumPointsByAccountId(Long accountId);
}
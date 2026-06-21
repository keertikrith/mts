package com.banking.transfer.repository;

import com.banking.transfer.entity.RewardLedger;
import com.banking.transfer.entity.RewardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardLedgerRepository extends JpaRepository<RewardLedger, Long> {

    // Used as idempotency guard, allowing one ledger entry per reward event type.
    boolean existsByTransactionIdAndRewardType(String transactionId, RewardType rewardType);

    // All reward entries for an account, newest first
    List<RewardLedger> findByAccountIdOrderByCreatedOnDesc(Long accountId);

    // Total points for an account
    @Query("SELECT COALESCE(SUM(r.pointsEarned), 0) FROM RewardLedger r WHERE r.accountId = ?1")
    int sumPointsByAccountId(Long accountId);
}

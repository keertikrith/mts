package com.banking.transfer.repository;

import com.banking.transfer.entity.RewardRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardRedemptionRepository extends JpaRepository<RewardRedemption, Long> {

    // All redemption entries for an account, newest first
    List<RewardRedemption> findByAccountIdOrderByRedeemedOnDesc(Long accountId);

    // Total points redeemed for an account
    @Query("SELECT COALESCE(SUM(r.pointsRedeemed), 0) FROM RewardRedemption r WHERE r.accountId = ?1")
    int sumPointsRedeemedByAccountId(Long accountId);
}

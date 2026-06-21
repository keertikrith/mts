package com.banking.transfer.service;

import com.banking.transfer.dto.RewardLedgerResponse;
import com.banking.transfer.dto.RewardSummaryResponse;
import com.banking.transfer.entity.RewardLedger;
import com.banking.transfer.entity.RewardType;
import com.banking.transfer.entity.TransactionLog;
import com.banking.transfer.entity.TransactionStatus;
import com.banking.transfer.repository.RewardLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RewardService {

    private static final BigDecimal MINIMUM_ELIGIBLE_AMOUNT = new BigDecimal("100");
    private static final BigDecimal POINTS_DIVISOR = new BigDecimal("100");

    private final RewardLedgerRepository rewardLedgerRepository;

    @Transactional
    public void redeemPoints(Long accountId, String transactionId, int pointsToDeduct) {
        log.info("Redeeming {} reward points from account {} for transaction {}", pointsToDeduct, accountId, transactionId);

        if (rewardLedgerRepository.existsByTransactionIdAndRewardType(transactionId, RewardType.REDEEM)) {
            log.warn("Transaction {} already has a redemption entry in the reward ledger. Skipping redemption.", transactionId);
            return;
        }

        RewardLedger ledger = RewardLedger.builder()
                .accountId(accountId)
                .transactionId(transactionId)
                .rewardType(RewardType.REDEEM)
                .pointsEarned(-pointsToDeduct)
                .build();

        rewardLedgerRepository.save(ledger);
    }

    @Transactional
    public void processReward(TransactionLog transactionLog, Long senderAccountId, BigDecimal cashAmountSpent) {
        String txId = transactionLog.getId();

        if (rewardLedgerRepository.existsByTransactionIdAndRewardType(txId, RewardType.EARN)) {
            log.warn("Transaction {} already has an earn entry in the reward ledger. Skipping processing.", txId);
            return;
        }

        // Rule: Base eligibility calculations strictly on net out-of-pocket cash spent
        if (transactionLog.getStatus() != TransactionStatus.SUCCESS
                || cashAmountSpent.compareTo(MINIMUM_ELIGIBLE_AMOUNT) <= 0
                || transactionLog.getFromAccountId().equals(transactionLog.getToAccountId())) {
            log.debug("Transaction {} is not eligible for earning new rewards.", txId);
            return;
        }

        int points = calculatePoints(cashAmountSpent);

        if (points <= 0) {
            return;
        }

        RewardLedger ledger = RewardLedger.builder()
                .accountId(senderAccountId)
                .transactionId(txId)
                .rewardType(RewardType.EARN)
                .pointsEarned(points)
                .build();

        rewardLedgerRepository.save(ledger);
        log.info("Reward granted: {} points to account {} based on cash spend of ₹{}", points, senderAccountId, cashAmountSpent);
    }

    @Transactional(readOnly = true)
    public RewardSummaryResponse getRewardSummary(Long accountId) {
        int totalPoints = rewardLedgerRepository.sumPointsByAccountId(accountId);
        List<RewardLedger> entries = rewardLedgerRepository.findByAccountIdOrderByCreatedOnDesc(accountId);

        List<RewardLedgerResponse> history = entries.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return RewardSummaryResponse.builder()
                .accountId(accountId)
                .totalPoints(totalPoints)
                .history(history)
                .build();
    }

    private int calculatePoints(BigDecimal amount) {
        return amount.divide(POINTS_DIVISOR, 0, java.math.RoundingMode.FLOOR).intValue();
    }

    private RewardLedgerResponse toResponse(RewardLedger ledger) {
        return RewardLedgerResponse.builder()
                .id(ledger.getId())
                .transactionId(ledger.getTransactionId())
                .rewardType(ledger.getRewardType())
                .pointsEarned(ledger.getPointsEarned())
                .createdOn(ledger.getCreatedOn())
                .build();
    }
}

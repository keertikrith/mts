package com.banking.transfer.service;

import com.banking.transfer.dto.RewardLedgerResponse;
import com.banking.transfer.dto.RewardSummaryResponse;
import com.banking.transfer.entity.RewardLedger;
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

    // Minimum amount for reward eligibility
    private static final BigDecimal MINIMUM_ELIGIBLE_AMOUNT = new BigDecimal("100");

    // 1 point per ₹100 transferred
    private static final BigDecimal POINTS_DIVISOR = new BigDecimal("100");

    private final RewardLedgerRepository rewardLedgerRepository;

    /**
     * Evaluates a completed transaction for reward eligibility and grants points
     * if all conditions are met. Idempotent — safe to call multiple times for the
     * same transaction.
     *
     * Eligibility rules:
     *   1. Transaction status must be SUCCESS
     *   2. Amount must be > 100
     *   3. Sender and receiver must be different accounts (self-transfer guard)
     *
     * Called by TransferService immediately after a successful transaction save.
     *
     * @param transactionLog  the saved transaction to evaluate
     * @param senderAccountId the account that sent the money (receives the reward)
     */
    @Transactional
    public void processReward(TransactionLog transactionLog, Long senderAccountId) {
        String txId = transactionLog.getId();

        // Idempotency guard — never grant reward twice for the same transaction
        if (rewardLedgerRepository.findByTransactionId(txId).isPresent()) {
            log.warn("Reward already processed for transaction {}. Skipping.", txId);
            return;
        }

        if (!isEligible(transactionLog)) {
            log.debug("Transaction {} is not eligible for rewards.", txId);
            return;
        }

        int points = calculatePoints(transactionLog.getAmount());

        if (points <= 0) {
            log.debug("Transaction {} yields 0 points. No reward granted.", txId);
            return;
        }

        RewardLedger ledger = RewardLedger.builder()
                .accountId(senderAccountId)
                .transactionId(txId)
                .pointsEarned(points)
                .build();

        rewardLedgerRepository.save(ledger);

        log.info("Reward granted: {} points to account {} for transaction {}", points, senderAccountId, txId);
    }

    /**
     * Returns total reward points and full reward history for an account.
     */
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

    // ── Private helpers ──────────────────────────────────────────────────────────

    private boolean isEligible(TransactionLog tx) {
        // Rule 1: Must be a successful transaction
        if (tx.getStatus() != TransactionStatus.SUCCESS) {
            return false;
        }

        // Rule 2: Amount must be strictly greater than 100
        if (tx.getAmount().compareTo(MINIMUM_ELIGIBLE_AMOUNT) <= 0) {
            return false;
        }

        // Rule 3: Sender and receiver must be different (no self-transfers)
        // TransferService already validates this, but we guard here too for safety
        if (tx.getFromAccountId().equals(tx.getToAccountId())) {
            return false;
        }

        return true;
    }

    /**
     * 1 point per ₹100, floored.
     * e.g. ₹250 → 2 points, ₹199 → 1 point
     */
    private int calculatePoints(BigDecimal amount) {
        return amount.divide(POINTS_DIVISOR, 0, java.math.RoundingMode.FLOOR).intValue();
    }

    private RewardLedgerResponse toResponse(RewardLedger ledger) {
        return RewardLedgerResponse.builder()
                .id(ledger.getId())
                .transactionId(ledger.getTransactionId())
                .pointsEarned(ledger.getPointsEarned())
                .createdOn(ledger.getCreatedOn())
                .build();
    }
}
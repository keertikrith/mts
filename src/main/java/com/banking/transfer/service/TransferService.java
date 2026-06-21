package com.banking.transfer.service;

import com.banking.transfer.dto.TransferRequest;
import com.banking.transfer.dto.TransferResponse;
import com.banking.transfer.entity.Account;
import com.banking.transfer.entity.TransactionLog;
import com.banking.transfer.entity.TransactionStatus;
import com.banking.transfer.exception.*;
import com.banking.transfer.repository.AccountRepository;
import com.banking.transfer.repository.TransactionLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Slf4j
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final RewardService rewardService;

    public TransferService(AccountRepository accountRepository,
                           TransactionLogRepository transactionLogRepository,
                           RewardService rewardService) {
        this.accountRepository = accountRepository;
        this.transactionLogRepository = transactionLogRepository;
        this.rewardService = rewardService;
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        log.info("Processing transfer from {} to {} for total amount ₹{}. Reward redemption: {}",
                request.getFromAccountId(), request.getToAccountId(), request.getAmount(), request.isRedeemRewards());

        validateTransferRequest(request);

        if (transactionLogRepository.findByIdempotencyKey(request.getIdempotencyKey()).isPresent()) {
            throw new DuplicateTransferException(
                    "Duplicate transfer request with idempotency key: " + request.getIdempotencyKey());
        }

        BigDecimal totalTransferAmount = request.getAmount();
        BigDecimal rewardDiscountValue = BigDecimal.ZERO;
        BigDecimal netCashRequired = totalTransferAmount;
        int pointsToDeduct = 0;

        try {
            Account fromAccount = accountRepository.findById(request.getFromAccountId())
                    .orElseThrow(() -> new AccountNotFoundException("Source account not found"));
            Account toAccount = accountRepository.findById(request.getToAccountId())
                    .orElseThrow(() -> new AccountNotFoundException("Destination account not found"));

            if (!fromAccount.isActive() || !toAccount.isActive()) {
                throw new AccountNotActiveException("One or both accounts are not active");
            }

            // --- REDEMPTION SCHEDULING DISCOUNTS ---
            if (request.isRedeemRewards()) {
                int availablePoints = rewardService.getRewardSummary(request.getFromAccountId()).getTotalPoints();

                if (availablePoints > 0) {
                    // CHANGED: 1 points = 1 Rupee, so total points required is Amount * 1
                    int pointsNeededForFullAmount = totalTransferAmount.multiply(new BigDecimal("1")).intValue();

                    // Use either what is available or what is needed to cover the total amount
                    pointsToDeduct = Math.min(availablePoints, pointsNeededForFullAmount);
                    rewardDiscountValue = new BigDecimal(pointsToDeduct).divide(new BigDecimal("1"));
                    netCashRequired = totalTransferAmount.subtract(rewardDiscountValue);
                }
            }

            // Validate that checking account balance can cover the remaining cash portion
            if (fromAccount.getBalance().compareTo(netCashRequired) < 0) {
                throw new InsufficientBalanceException("Insufficient balance to cover the remaining cash total of ₹" + netCashRequired);
            }

            // Debit from source bank account (Only the cash portion)
            if (netCashRequired.compareTo(BigDecimal.ZERO) > 0) {
                fromAccount.debit(netCashRequired);
                accountRepository.save(fromAccount);
            }

            // Save baseline transaction log
            TransactionLog transactionLog = TransactionLog.builder()
                    .fromAccountId(request.getFromAccountId())
                    .toAccountId(request.getToAccountId())
                    .amount(totalTransferAmount)
                    .status(TransactionStatus.SUCCESS)
                    .idempotencyKey(request.getIdempotencyKey())
                    .failureReason(pointsToDeduct > 0 ? "REDEEMED_POINTS: " + pointsToDeduct : null)
                    .build();
            transactionLog = transactionLogRepository.save(transactionLog);

            // Commit points ledger deduction if rewards were used
            if (pointsToDeduct > 0) {
                rewardService.redeemPoints(request.getFromAccountId(), transactionLog.getId(), pointsToDeduct);
            }

            // Credit the full requested amount to the destination bank account
            toAccount.credit(totalTransferAmount);
            accountRepository.save(toAccount);

            // Process reward earnings based ONLY on net out-of-pocket cash spent
            rewardService.processReward(transactionLog, request.getFromAccountId(), netCashRequired);

            return TransferResponse.builder()
                    .transactionId(transactionLog.getId())
                    .status("SUCCESS")
                    .message(pointsToDeduct > 0
                            ? String.format("Transferred successfully! Applied ₹%s discount via rewards.", rewardDiscountValue)
                            : "Transfer completed successfully")
                    .debitedFrom(request.getFromAccountId())
                    .creditedTo(request.getToAccountId())
                    .amount(totalTransferAmount)
                    .build();

        } catch (Exception e) {
            TransactionLog failedLog = TransactionLog.builder()
                    .fromAccountId(request.getFromAccountId())
                    .toAccountId(request.getToAccountId())
                    .amount(totalTransferAmount)
                    .status(TransactionStatus.FAILED)
                    .failureReason(e.getMessage())
                    .idempotencyKey(request.getIdempotencyKey())
                    .build();
            transactionLogRepository.save(failedLog);
            throw e;
        }
    }

    private void validateTransferRequest(TransferRequest request) {
        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        if (request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
    }
}

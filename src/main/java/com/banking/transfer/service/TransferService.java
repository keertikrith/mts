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

@Service
@Slf4j
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;

    public TransferService(AccountRepository accountRepository,
            TransactionLogRepository transactionLogRepository) {
        this.accountRepository = accountRepository;
        this.transactionLogRepository = transactionLogRepository;
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        log.info("Processing transfer from {} to {} for amount {}",
                request.getFromAccountId(), request.getToAccountId(), request.getAmount());

        // Validate request
        validateTransferRequest(request);

        // Check for duplicate idempotency key
        if (transactionLogRepository.findByIdempotencyKey(request.getIdempotencyKey()).isPresent()) {
            throw new DuplicateTransferException(
                    "Duplicate transfer request with idempotency key: " + request.getIdempotencyKey());
        }

        try {
            // Get accounts with pessimistic locking
            Account fromAccount = accountRepository.findById(request.getFromAccountId())
                    .orElseThrow(() -> new AccountNotFoundException(
                            "Source account not found: " + request.getFromAccountId()));

            Account toAccount = accountRepository.findById(request.getToAccountId())
                    .orElseThrow(() -> new AccountNotFoundException(
                            "Destination account not found: " + request.getToAccountId()));

            // Validate account status
            if (!fromAccount.isActive()) {
                throw new AccountNotActiveException("Source account is not active");
            }

            if (!toAccount.isActive()) {
                throw new AccountNotActiveException("Destination account is not active");
            }

            // Validate sufficient balance
            if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
                throw new InsufficientBalanceException("Insufficient balance in source account");
            }

            // Execute transfer (debit before credit)
            fromAccount.debit(request.getAmount());
            toAccount.credit(request.getAmount());

            // Save accounts
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            // Log successful transaction
            TransactionLog transactionLog = TransactionLog.builder()
                    .fromAccountId(request.getFromAccountId())
                    .toAccountId(request.getToAccountId())
                    .amount(request.getAmount())
                    .status(TransactionStatus.SUCCESS)
                    .idempotencyKey(request.getIdempotencyKey())
                    .build();

            TransactionLog savedLog = transactionLogRepository.save(transactionLog);

            log.info("Transfer completed successfully. Transaction ID: {}", savedLog.getId());

            return TransferResponse.builder()
                    .transactionId(savedLog.getId())
                    .status("SUCCESS")
                    .message("Transfer completed successfully")
                    .debitedFrom(request.getFromAccountId())
                    .creditedTo(request.getToAccountId())
                    .amount(request.getAmount())
                    .build();

        } catch (Exception e) {
            // Log failed transaction
            TransactionLog failedLog = TransactionLog.builder()
                    .fromAccountId(request.getFromAccountId())
                    .toAccountId(request.getToAccountId())
                    .amount(request.getAmount())
                    .status(TransactionStatus.FAILED)
                    .failureReason(e.getMessage())
                    .idempotencyKey(request.getIdempotencyKey())
                    .build();

            transactionLogRepository.save(failedLog);

            log.error("Transfer failed: {}", e.getMessage());
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

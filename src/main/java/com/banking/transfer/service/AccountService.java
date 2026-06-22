package com.banking.transfer.service;

import com.banking.transfer.dto.AccountResponse;
import com.banking.transfer.dto.CreateAccountRequest;
import com.banking.transfer.dto.LoginRequest;
import com.banking.transfer.dto.TransactionResponse;
import com.banking.transfer.entity.Account;
import com.banking.transfer.entity.AccountStatus;
import com.banking.transfer.entity.TransactionLog;
import com.banking.transfer.exception.AccountNotFoundException;
import com.banking.transfer.exception.DuplicateUsernameException;
import com.banking.transfer.exception.InvalidCredentialsException;
import com.banking.transfer.repository.AccountRepository;
import com.banking.transfer.repository.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final SnowflakeSyncService snowflakeSyncService;

    @Transactional
    public ResponseEntity<AccountResponse> createAccount(CreateAccountRequest request) {
        log.info("Creating account for username: {}", request.getUsername());

        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUsernameException("Username '" + request.getUsername() + "' is already taken");
        }

        Account account = Account.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .holderName(request.getHolderName())
                .balance(request.getInitialBalance())
                .status(AccountStatus.ACTIVE)
                .build();

        Account savedAccount = accountRepository.save(account);
        log.info("Account created successfully with ID: {}", savedAccount.getId());

        // Sync new account to Snowflake asynchronously
        snowflakeSyncService.syncAccount(savedAccount);

        return ResponseEntity.status(HttpStatus.CREATED).body(toAccountResponse(savedAccount));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<AccountResponse> login(LoginRequest request) {
        log.info("Login attempt for username: {}", request.getUsername());

        Account account = accountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        log.info("Login successful for username: {}", request.getUsername());
        return ResponseEntity.ok(toAccountResponse(account));
    }

    @Transactional(readOnly = true)
    public Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account with ID " + accountId + " not found"));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<AccountResponse> getAccountResponse(Long accountId) {
        Account account = getAccount(accountId);
        return ResponseEntity.ok(toAccountResponse(account));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(Long accountId) {
        getAccount(accountId);

        List<TransactionLog> transactions = transactionLogRepository.findByAccountId(accountId);

        return transactions.stream()
                .map(t -> {
                    TransactionResponse response = TransactionResponse.builder()
                            .id(t.getId())
                            .fromAccountId(t.getFromAccountId())
                            .toAccountId(t.getToAccountId())
                            .amount(t.getAmount())
                            .status(t.getStatus())
                            .failureReason(t.getFailureReason())
                            .createdOn(t.getCreatedOn())
                            .build();

                    response.setType(t.getFromAccountId().equals(accountId) ? "DEBIT" : "CREDIT");
                    return response;
                })
                .collect(Collectors.toList());
    }

    private AccountResponse toAccountResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(String.format("ACC-%06d", account.getId()))
                .username(account.getUsername())
                .holderName(account.getHolderName())
                .balance(account.getBalance())
                .status(account.getStatus())
                .build();
    }
}

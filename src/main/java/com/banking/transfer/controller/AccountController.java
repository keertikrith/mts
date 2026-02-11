package com.banking.transfer.controller;

import com.banking.transfer.dto.AccountResponse;
import com.banking.transfer.dto.CreateAccountRequest;
import com.banking.transfer.dto.LoginRequest;
import com.banking.transfer.dto.TransactionResponse;
import com.banking.transfer.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.createAccount(request);
    }

    @PostMapping("/login")
    public ResponseEntity<AccountResponse> login(@Valid @RequestBody LoginRequest request) {
        return accountService.login(request);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long id) {
        return accountService.getAccountResponse(id);
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<AccountResponse> getBalance(@PathVariable Long id) {
        return accountService.getAccountResponse(id);
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(@PathVariable Long id) {
        List<TransactionResponse> transactions = accountService.getTransactions(id);
        return ResponseEntity.ok(transactions);
    }

}

    
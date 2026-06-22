package com.banking.transfer.controller;

import com.banking.transfer.dto.AccountResponse;
import com.banking.transfer.dto.CreateAccountRequest;
import com.banking.transfer.dto.ErrorResponse;
import com.banking.transfer.dto.LoginRequest;
import com.banking.transfer.dto.TransactionResponse;
import com.banking.transfer.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Accounts", description = "Account management: registration, login, balance & transaction history")
public class AccountController {

    private final AccountService accountService;

    @Operation(
            summary = "Register a new account",
            description = "Creates a new bank account. Password is hashed (BCrypt) before storage. The username must be unique.",
            security = {}  // public endpoint — no auth required
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "409", description = "Username already taken (ACC-409)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Validation failed — missing/invalid fields (VAL-422)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.createAccount(request);
    }

    @Operation(
            summary = "Login to an existing account",
            description = """
                    Authenticates a user using username + password. 
                    Returns the account details on success.
                    Both "user not found" and "wrong password" return the same AUTH-401 to prevent username enumeration.
                    """,
            security = {}  // public endpoint — no auth required
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid username or password (AUTH-401)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Validation failed — missing fields (VAL-422)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<AccountResponse> login(@Valid @RequestBody LoginRequest request) {
        return accountService.login(request);
    }

    @Operation(
            summary = "Get account details by ID",
            description = "Returns account info including current balance and status.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account found",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found (ACC-404)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(
            @Parameter(description = "Account ID", example = "1") @PathVariable Long id) {
        return accountService.getAccountResponse(id);
    }

    @Operation(
            summary = "Get account balance",
            description = "Returns the current balance for the specified account.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balance retrieved",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found (ACC-404)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/balance")
    public ResponseEntity<AccountResponse> getBalance(
            @Parameter(description = "Account ID", example = "1") @PathVariable Long id) {
        return accountService.getAccountResponse(id);
    }

    @Operation(
            summary = "Get transaction history",
            description = "Returns all transactions (DEBIT and CREDIT) for the specified account, ordered by most recent first.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction history retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransactionResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Account not found (ACC-404)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @Parameter(description = "Account ID", example = "1") @PathVariable Long id) {
        List<TransactionResponse> transactions = accountService.getTransactions(id);
        return ResponseEntity.ok(transactions);
    }

}
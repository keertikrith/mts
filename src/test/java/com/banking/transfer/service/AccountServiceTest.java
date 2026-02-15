package com.banking.transfer.service;

import com.banking.transfer.dto.AccountResponse;
import com.banking.transfer.dto.CreateAccountRequest;
import com.banking.transfer.dto.LoginRequest;
import com.banking.transfer.entity.Account;
import com.banking.transfer.entity.AccountStatus;
import com.banking.transfer.exception.DuplicateUsernameException;
import com.banking.transfer.exception.InvalidCredentialsException;
import com.banking.transfer.repository.AccountRepository;
import com.banking.transfer.repository.TransactionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    private CreateAccountRequest createAccountRequest;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        createAccountRequest = CreateAccountRequest.builder()
                .username("testuser")
                .password("password123")
                .holderName("Test User")
                .initialBalance(new BigDecimal("1000.00"))
                .build();

        testAccount = Account.builder()
                .id(1L)
                .username("testuser")
                .password("$2a$10$encoded_password")
                .holderName("Test User")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .version(0)
                .build();
    }

    @Test
    void createAccount_Success() {
        when(accountRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded_password");
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        ResponseEntity<AccountResponse> responseEntity = accountService.createAccount(createAccountRequest);
        AccountResponse response = responseEntity.getBody();

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("Test User", response.getHolderName());
        assertEquals(new BigDecimal("1000.00"), response.getBalance());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());
        assertEquals("ACC-000001", response.getAccountNumber());

        verify(accountRepository, times(1)).existsByUsername("testuser");
        verify(passwordEncoder, times(1)).encode("password123");
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void createAccount_DuplicateUsername_ThrowsException() {
        when(accountRepository.existsByUsername(anyString())).thenReturn(true);

        DuplicateUsernameException exception = assertThrows(
                DuplicateUsernameException.class,
                () -> accountService.createAccount(createAccountRequest));

        assertTrue(exception.getMessage().contains("testuser"));
        verify(accountRepository, times(1)).existsByUsername("testuser");
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void login_ValidCredentials_Success() {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches("password123", "$2a$10$encoded_password")).thenReturn(true);

        ResponseEntity<AccountResponse> responseEntity = accountService.login(loginRequest);
        AccountResponse response = responseEntity.getBody();

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals(1L, response.getId());

        verify(accountRepository, times(1)).findByUsername("testuser");
        verify(passwordEncoder, times(1)).matches("password123", "$2a$10$encoded_password");
    }

    @Test
    void login_InvalidUsername_ThrowsException() {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("nonexistent")
                .password("password123")
                .build();

        when(accountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> accountService.login(loginRequest));

        assertEquals("Invalid username or password", exception.getMessage());
        verify(accountRepository, times(1)).findByUsername("nonexistent");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("wrongpassword")
                .build();

        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches("wrongpassword", "$2a$10$encoded_password")).thenReturn(false);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> accountService.login(loginRequest));

        assertEquals("Invalid username or password", exception.getMessage());
        verify(accountRepository, times(1)).findByUsername("testuser");
        verify(passwordEncoder, times(1)).matches("wrongpassword", "$2a$10$encoded_password");
    }

    @Test
    void getAccountResponse_Success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        ResponseEntity<AccountResponse> responseEntity = accountService.getAccountResponse(1L);
        AccountResponse response = responseEntity.getBody();

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("testuser", response.getUsername());
        assertEquals("Test User", response.getHolderName());
        assertEquals(new BigDecimal("1000.00"), response.getBalance());

        verify(accountRepository, times(1)).findById(1L);
    }

    @Test
    void getAccountResponse_AccountNotFound_ThrowsException() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                com.banking.transfer.exception.AccountNotFoundException.class,
                () -> accountService.getAccountResponse(999L));

        verify(accountRepository, times(1)).findById(999L);
    }

    @Test
    void getAccount_Success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        Account result = accountService.getAccount(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        verify(accountRepository, times(1)).findById(1L);
    }

    @Test
    void getAccount_NotFound_ThrowsException() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                com.banking.transfer.exception.AccountNotFoundException.class,
                () -> accountService.getAccount(999L));

        verify(accountRepository, times(1)).findById(999L);
    }

    @Test
    void getTransactions_EmptyList() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionLogRepository.findByAccountId(1L)).thenReturn(java.util.Collections.emptyList());

        java.util.List<com.banking.transfer.dto.TransactionResponse> transactions = accountService.getTransactions(1L);

        assertNotNull(transactions);
        assertTrue(transactions.isEmpty());
        verify(accountRepository, times(1)).findById(1L);
        verify(transactionLogRepository, times(1)).findByAccountId(1L);
    }

    @Test
    void getTransactions_WithDebitTransactions() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        com.banking.transfer.entity.TransactionLog debitLog = com.banking.transfer.entity.TransactionLog.builder()
                .id("txn-1")
                .fromAccountId(1L)
                .toAccountId(2L)
                .amount(new BigDecimal("100.00"))
                .status(com.banking.transfer.entity.TransactionStatus.SUCCESS)
                .createdOn(java.time.LocalDateTime.now())
                .build();

        when(transactionLogRepository.findByAccountId(1L)).thenReturn(java.util.List.of(debitLog));

        java.util.List<com.banking.transfer.dto.TransactionResponse> transactions = accountService.getTransactions(1L);

        assertNotNull(transactions);
        assertEquals(1, transactions.size());
        assertEquals("DEBIT", transactions.get(0).getType());
        assertEquals(new BigDecimal("100.00"), transactions.get(0).getAmount());
    }

    @Test
    void getTransactions_WithCreditTransactions() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        com.banking.transfer.entity.TransactionLog creditLog = com.banking.transfer.entity.TransactionLog.builder()
                .id("txn-2")
                .fromAccountId(2L)
                .toAccountId(1L)
                .amount(new BigDecimal("200.00"))
                .status(com.banking.transfer.entity.TransactionStatus.SUCCESS)
                .createdOn(java.time.LocalDateTime.now())
                .build();

        when(transactionLogRepository.findByAccountId(1L)).thenReturn(java.util.List.of(creditLog));

        java.util.List<com.banking.transfer.dto.TransactionResponse> transactions = accountService.getTransactions(1L);

        assertNotNull(transactions);
        assertEquals(1, transactions.size());
        assertEquals("CREDIT", transactions.get(0).getType());
        assertEquals(new BigDecimal("200.00"), transactions.get(0).getAmount());
    }

    @Test
    void getTransactions_AccountNotFound() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                com.banking.transfer.exception.AccountNotFoundException.class,
                () -> accountService.getTransactions(999L));

        verify(accountRepository, times(1)).findById(999L);
    }

}

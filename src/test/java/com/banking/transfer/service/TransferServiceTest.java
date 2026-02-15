package com.banking.transfer.service;

import com.banking.transfer.dto.TransferRequest;
import com.banking.transfer.dto.TransferResponse;
import com.banking.transfer.entity.Account;
import com.banking.transfer.entity.AccountStatus;
import com.banking.transfer.entity.TransactionLog;
import com.banking.transfer.exception.AccountNotFoundException;
import com.banking.transfer.exception.DuplicateTransferException;
import com.banking.transfer.exception.InsufficientBalanceException;
import com.banking.transfer.repository.AccountRepository;
import com.banking.transfer.repository.TransactionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @InjectMocks
    private TransferService transferService;

    private Account fromAccount;
    private Account toAccount;
    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        fromAccount = Account.builder()
                .id(1L)
                .username("alice")
                .password("encoded_password")
                .holderName("Alice Johnson")
                .balance(new BigDecimal("5000.00"))
                .status(AccountStatus.ACTIVE)
                .version(0)
                .build();

        toAccount = Account.builder()
                .id(2L)
                .username("bob")
                .password("encoded_password")
                .holderName("Bob Smith")
                .balance(new BigDecimal("3000.00"))
                .status(AccountStatus.ACTIVE)
                .version(0)
                .build();

        transferRequest = TransferRequest.builder()
                .fromAccountId(1L)
                .toAccountId(2L)
                .amount(new BigDecimal("500.00"))
                .idempotencyKey("txn-001")
                .build();
    }

    @Test
    void transfer_Success() {
        // Arrange
        when(transactionLogRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(invocation -> {
            TransactionLog log = invocation.getArgument(0);
            log.setId("txn-id-123");
            return log;
        });

        // Act
        TransferResponse response = transferService.transfer(transferRequest);

        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(new BigDecimal("500.00"), response.getAmount());

        // Verify balances updated
        assertEquals(new BigDecimal("4500.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("3500.00"), toAccount.getBalance());

        verify(transactionLogRepository, times(1)).findByIdempotencyKey("txn-001");
        verify(accountRepository, times(2)).findById(anyLong());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionLogRepository, times(1)).save(any(TransactionLog.class));
    }

    @Test
    void transfer_InsufficientBalance_ThrowsException() {
        // Arrange
        transferRequest.setAmount(new BigDecimal("10000.00")); // More than balance

        when(transactionLogRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        // Act & Assert
        InsufficientBalanceException exception = assertThrows(
                InsufficientBalanceException.class,
                () -> transferService.transfer(transferRequest));

        assertTrue(exception.getMessage().contains("Insufficient balance"));

        verify(transactionLogRepository, times(1)).findByIdempotencyKey("txn-001");
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void transfer_DuplicateIdempotencyKey_ThrowsException() {
        // Arrange
        TransactionLog existingLog = new TransactionLog();
        when(transactionLogRepository.findByIdempotencyKey("txn-001")).thenReturn(Optional.of(existingLog));

        // Act & Assert
        DuplicateTransferException exception = assertThrows(
                DuplicateTransferException.class,
                () -> transferService.transfer(transferRequest));

        assertTrue(exception.getMessage().contains("Duplicate"));

        verify(transactionLogRepository, times(1)).findByIdempotencyKey("txn-001");
        verify(accountRepository, never()).findById(anyLong());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void transfer_SameAccount_ThrowsException() {
        // Arrange
        transferRequest.setToAccountId(1L); // Same as fromAccountId

        // No mocking needed - validation happens before any repository calls

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transferService.transfer(transferRequest));

        assertTrue(exception.getMessage().contains("Cannot transfer to the same account"));

        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionLogRepository, never()).save(any(TransactionLog.class));
    }

    @Test
    void transfer_FromAccountNotFound_ThrowsException() {
        // Arrange
        when(transactionLogRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        AccountNotFoundException exception = assertThrows(
                AccountNotFoundException.class,
                () -> transferService.transfer(transferRequest));

        assertTrue(exception.getMessage().contains("not found"));

        verify(accountRepository, times(1)).findById(1L);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void transfer_ToAccountNotFound_ThrowsException() {
        // Arrange
        when(transactionLogRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.empty());

        // Act & Assert
        AccountNotFoundException exception = assertThrows(
                AccountNotFoundException.class,
                () -> transferService.transfer(transferRequest));

        assertTrue(exception.getMessage().contains("not found"));

        verify(accountRepository, times(1)).findById(2L);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void transfer_FromAccountInactive_ThrowsException() {
        // Arrange
        fromAccount.setStatus(AccountStatus.LOCKED);
        when(transactionLogRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        // Act & Assert
        com.banking.transfer.exception.AccountNotActiveException exception = assertThrows(
                com.banking.transfer.exception.AccountNotActiveException.class,
                () -> transferService.transfer(transferRequest));

        assertTrue(exception.getMessage().contains("not active"));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void transfer_ToAccountInactive_ThrowsException() {
        // Arrange
        toAccount.setStatus(AccountStatus.LOCKED);
        when(transactionLogRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        // Act & Assert
        com.banking.transfer.exception.AccountNotActiveException exception = assertThrows(
                com.banking.transfer.exception.AccountNotActiveException.class,
                () -> transferService.transfer(transferRequest));

        assertTrue(exception.getMessage().contains("not active"));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void transfer_NegativeAmount_ThrowsException() {
        // Arrange
        transferRequest.setAmount(new BigDecimal("-100.00"));

        // No mocking needed - validation happens before any repository calls

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transferService.transfer(transferRequest));

        assertTrue(exception.getMessage().contains("positive"));
        verify(accountRepository, never()).findById(anyLong());
    }

    @Test
    void transfer_ZeroAmount_ThrowsException() {
        // Arrange
        transferRequest.setAmount(BigDecimal.ZERO);

        // No mocking needed - validation happens before any repository calls

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transferService.transfer(transferRequest));

        assertTrue(exception.getMessage().contains("positive"));
        verify(accountRepository, never()).findById(anyLong());
    }

    @Test
    void transfer_FailureLogsTransaction() {
        // Arrange
        fromAccount.setBalance(new BigDecimal("100.00"));
        transferRequest.setAmount(new BigDecimal("500.00"));

        when(transactionLogRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act & Assert
        assertThrows(
                InsufficientBalanceException.class,
                () -> transferService.transfer(transferRequest));

        verify(transactionLogRepository, times(1)).save(any(TransactionLog.class));
    }
}

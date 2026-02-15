package com.banking.transfer.security;

import com.banking.transfer.entity.Account;
import com.banking.transfer.entity.AccountStatus;
import com.banking.transfer.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
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
    void loadUserByUsername_Success() {
        // Arrange
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("$2a$10$encoded_password", userDetails.getPassword());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.getAuthorities().isEmpty());
        verify(accountRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_UserNotFound_ThrowsException() {
        // Arrange
        when(accountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("nonexistent"));

        assertTrue(exception.getMessage().contains("User not found"));
        assertTrue(exception.getMessage().contains("nonexistent"));
        verify(accountRepository, times(1)).findByUsername("nonexistent");
    }

    @Test
    void loadUserByUsername_LockedAccount() {
        // Arrange
        testAccount.setStatus(AccountStatus.LOCKED);
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertFalse(userDetails.isAccountNonLocked());
        verify(accountRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_ClosedAccount() {
        // Arrange
        testAccount.setStatus(AccountStatus.CLOSED);
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertFalse(userDetails.isAccountNonLocked());
        verify(accountRepository, times(1)).findByUsername("testuser");
    }
}

package com.banking.transfer.controller;

import com.banking.transfer.dto.AccountResponse;
import com.banking.transfer.dto.CreateAccountRequest;
import com.banking.transfer.dto.LoginRequest;
import com.banking.transfer.entity.AccountStatus;
import com.banking.transfer.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private AccountService accountService;

        @Test
        void createAccount_Success() throws Exception {
                // Arrange
                CreateAccountRequest request = CreateAccountRequest.builder()
                                .username("testuser")
                                .password("password123")
                                .holderName("Test User")
                                .initialBalance(new BigDecimal("1000.00"))
                                .build();

                AccountResponse response = AccountResponse.builder()
                                .id(1L)
                                .username("testuser")
                                .holderName("Test User")
                                .balance(new BigDecimal("1000.00"))
                                .status(AccountStatus.ACTIVE)
                                .build();

                when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(
                                ResponseEntity.status(HttpStatus.CREATED).body(response));

                // Act & Assert
                mockMvc.perform(post("/api/v1/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.username").value("testuser"))
                                .andExpect(jsonPath("$.holderName").value("Test User"))
                                .andExpect(jsonPath("$.balance").value(1000.00))
                                .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        void login_Success() throws Exception {
                // Arrange
                LoginRequest request = LoginRequest.builder()
                                .username("testuser")
                                .password("password123")
                                .build();

                AccountResponse response = AccountResponse.builder()
                                .id(1L)
                                .username("testuser")
                                .holderName("Test User")
                                .balance(new BigDecimal("1000.00"))
                                .status(AccountStatus.ACTIVE)
                                .build();

                when(accountService.login(any(LoginRequest.class))).thenReturn(ResponseEntity.ok(response));

                // Act & Assert
                mockMvc.perform(post("/api/v1/accounts/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.username").value("testuser"));
        }

        @Test
        void getAccount_Success() throws Exception {
                // Arrange
                AccountResponse response = AccountResponse.builder()
                                .id(1L)
                                .username("testuser")
                                .holderName("Test User")
                                .balance(new BigDecimal("1000.00"))
                                .status(AccountStatus.ACTIVE)
                                .build();

                when(accountService.getAccountResponse(1L)).thenReturn(ResponseEntity.ok(response));

                // Act & Assert
                mockMvc.perform(get("/api/v1/accounts/1")
                                .header("Authorization", "Basic dGVzdHVzZXI6cGFzc3dvcmQxMjM="))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.username").value("testuser"))
                                .andExpect(jsonPath("$.balance").value(1000.00));
        }

        @Test
        void createAccount_InvalidRequest_MissingUsername() throws Exception {
                // Arrange
                CreateAccountRequest request = CreateAccountRequest.builder()
                                .password("password123")
                                .holderName("Test User")
                                .initialBalance(new BigDecimal("1000.00"))
                                .build();

                // Act & Assert
                mockMvc.perform(post("/api/v1/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnprocessableEntity());
        }

        @Test
        void createAccount_InvalidRequest_NegativeBalance() throws Exception {
                // Arrange
                CreateAccountRequest request = CreateAccountRequest.builder()
                                .username("testuser")
                                .password("password123")
                                .holderName("Test User")
                                .initialBalance(new BigDecimal("-100.00"))
                                .build();

                // Act & Assert
                mockMvc.perform(post("/api/v1/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnprocessableEntity());
        }
}

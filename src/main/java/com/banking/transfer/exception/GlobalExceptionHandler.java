package com.banking.transfer.exception;

import com.banking.transfer.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        @ExceptionHandler(AccountNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex) {
                log.error("Account not found: {}", ex.getMessage());
                ErrorResponse error = ErrorResponse.builder()
                                .errorCode("ACC-404")
                                .message(ex.getMessage())
                                .timestamp(System.currentTimeMillis())
                                .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        @ExceptionHandler(AccountNotActiveException.class)
        public ResponseEntity<ErrorResponse> handleAccountNotActive(AccountNotActiveException ex) {
                log.error("Account not active: {}", ex.getMessage());
                ErrorResponse error = ErrorResponse.builder()
                                .errorCode("ACC-403")
                                .message(ex.getMessage())
                                .timestamp(System.currentTimeMillis())
                                .build();
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        @ExceptionHandler(InsufficientBalanceException.class)
        public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex) {
                log.error("Insufficient balance: {}", ex.getMessage());
                ErrorResponse error = ErrorResponse.builder()
                                .errorCode("TRX-400")
                                .message(ex.getMessage())
                                .timestamp(System.currentTimeMillis())
                                .build();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(DuplicateTransferException.class)
        public ResponseEntity<ErrorResponse> handleDuplicateTransfer(DuplicateTransferException ex) {
                log.error("Duplicate transfer: {}", ex.getMessage());
                ErrorResponse error = ErrorResponse.builder()
                                .errorCode("TRX-409")
                                .message(ex.getMessage())
                                .timestamp(System.currentTimeMillis())
                                .build();
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        @ExceptionHandler(DuplicateUsernameException.class)
        public ResponseEntity<ErrorResponse> handleDuplicateUsername(DuplicateUsernameException ex) {
                log.error("Duplicate username: {}", ex.getMessage());
                ErrorResponse error = ErrorResponse.builder()
                                .errorCode("ACC-409")
                                .message(ex.getMessage())
                                .timestamp(System.currentTimeMillis())
                                .build();
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
                log.error("Invalid credentials: {}", ex.getMessage());
                ErrorResponse error = ErrorResponse.builder()
                                .errorCode("AUTH-401")
                                .message(ex.getMessage())
                                .timestamp(System.currentTimeMillis())
                                .build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
                log.error("Validation error: {}", ex.getMessage());
                ErrorResponse error = ErrorResponse.builder()
                                .errorCode("VAL-422")
                                .message(ex.getMessage())
                                .timestamp(System.currentTimeMillis())
                                .build();
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
                String message = ex.getBindingResult().getAllErrors().stream()
                                .map(error -> error instanceof FieldError fieldError
                                                ? fieldError.getField() + ": " + error.getDefaultMessage()
                                                : error.getDefaultMessage())
                                .collect(Collectors.joining(", "));

                log.error("Validation failed: {}", message);
                ErrorResponse error = ErrorResponse.builder()
                                .errorCode("VAL-422")
                                .message(message)
                                .timestamp(System.currentTimeMillis())
                                .build();
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorResponse> handleInvalidJsonInput(HttpMessageNotReadableException ex) {
                String message = "Invalid input format. Please check your request data.";

                // Extract more specific error message if available
                if (ex.getMessage() != null) {
                        if (ex.getMessage().contains("BigDecimal")) {
                                message = "Invalid number format. Please provide a valid numeric value for monetary fields.";
                        } else if (ex.getMessage().contains("JSON parse error")) {
                                message = "Invalid JSON format. Please check your request structure.";
                        }
                }

                log.error("Invalid JSON input: {}", ex.getMessage());
                ErrorResponse error = ErrorResponse.builder()
                                .errorCode("VAL-400")
                                .message(message)
                                .timestamp(System.currentTimeMillis())
                                .build();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
                log.error("Internal server error: {}", ex.getMessage(), ex);
                ErrorResponse error = ErrorResponse.builder()
                                .errorCode("SYS-500")
                                .message("An internal error occurred")
                                .timestamp(System.currentTimeMillis())
                                .build();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
}

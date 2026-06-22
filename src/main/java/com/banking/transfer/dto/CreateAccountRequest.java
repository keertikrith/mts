package com.banking.transfer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body for creating a new bank account")
public class CreateAccountRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "Unique username for login", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "Plain-text password (hashed with BCrypt before storage)", example = "SecurePass123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "Holder name is required")
    @Schema(description = "Full name of the account holder", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String holderName;

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Balance must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Balance must be a valid number")
    @Schema(description = "Opening balance (must be ≥ 0, max 2 decimal places)", example = "1000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal initialBalance;
}

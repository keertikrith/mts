package com.banking.transfer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
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
@Schema(description = "Request body for initiating a money transfer")
public class TransferRequest {

    @NotNull(message = "From account ID is required")
    @Schema(description = "ID of the source account (must be different from toAccountId)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fromAccountId;

    @NotNull(message = "To account ID is required")
    @Schema(description = "ID of the destination account", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long toAccountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Schema(description = "Amount to transfer in INR (must be > 0, up to 2 decimal places). Transfers > ₹100 earn reward points.", example = "500.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @NotBlank(message = "Idempotency key is required")
    @Schema(description = "Unique key to prevent duplicate transfers on network retries. Generate once per transfer attempt.", example = "1719000000000-abc123xyz", requiredMode = Schema.RequiredMode.REQUIRED)
    private String idempotencyKey;
}

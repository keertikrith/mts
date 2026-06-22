package com.banking.transfer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Standard error response returned for all API errors")
public class ErrorResponse {

    @Schema(description = "Application-specific error code", example = "TRX-409")
    private String errorCode;

    @Schema(description = "Human-readable error message", example = "Duplicate transfer request with idempotency key: 1719000000000-abc123xyz")
    private String message;

    @Schema(description = "Unix timestamp (milliseconds) when the error occurred", example = "1719000000000")
    private Long timestamp;
}

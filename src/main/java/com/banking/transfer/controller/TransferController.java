package com.banking.transfer.controller;

import com.banking.transfer.dto.ErrorResponse;
import com.banking.transfer.dto.TransferRequest;
import com.banking.transfer.dto.TransferResponse;
import com.banking.transfer.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Transfers", description = "Money transfer operations between accounts")
public class TransferController {

    private final TransferService transferService;

    @Operation(
            summary = "Transfer money between accounts",
            description = """
                    Transfers funds from one account to another. 
                    
                    **Validations performed:**
                    - `fromAccountId` ≠ `toAccountId` (no self-transfers)
                    - `amount` must be > 0
                    - Both accounts must exist and be ACTIVE
                    - Source account must have sufficient balance
                    - `idempotencyKey` must be unique — duplicate keys are rejected to prevent double-spending on network retries
                    
                    **Rewards:** Sender earns 1 point per ₹100 transferred (for amounts strictly > ₹100).
                    
                    **Audit:** Both SUCCESS and FAILED transactions are persisted to the transaction log.
                    """,
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfer successful",
                    content = @Content(schema = @Schema(implementation = TransferResponse.class))),
            @ApiResponse(responseCode = "400", description = "Insufficient balance (TRX-400) or amount ≤ 0 (VAL-422)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Source or destination account not active (ACC-403)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Source or destination account not found (ACC-404)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate transfer — idempotency key already used (TRX-409)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Validation failed — self-transfer, missing fields, or invalid amount (VAL-422)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        TransferResponse response = transferService.transfer(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}

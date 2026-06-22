package com.banking.transfer.controller;

import com.banking.transfer.dto.ErrorResponse;
import com.banking.transfer.dto.RewardRedemptionRequest;
import com.banking.transfer.dto.RewardRedemptionResponse;
import com.banking.transfer.dto.RewardSummaryResponse;
import com.banking.transfer.service.RewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Rewards", description = "Reward points: earning rules, history, and redemption")
public class RewardController {

    private final RewardService rewardService;

    @Operation(
            summary = "Get reward summary for an account",
            description = """
                    Returns total earned points, redeemed points, **available points**, 
                    and full earn + redemption history for the given account.
                    
                    **Earning rules:**
                    - Transfer amount must be **strictly > ₹100**
                    - Transaction must be SUCCESS
                    - Formula: `floor(amount / 100)` points (e.g. ₹250 → 2 pts, ₹199 → 1 pt)
                    - Points are only granted to the **sender** (not the receiver)
                    - One reward entry per transaction (idempotent)
                    """,
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reward summary retrieved",
                    content = @Content(schema = @Schema(implementation = RewardSummaryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found (ACC-404)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{accountId}")
    public ResponseEntity<RewardSummaryResponse> getRewards(
            @Parameter(description = "Account ID to fetch rewards for", example = "1")
            @PathVariable Long accountId) {
        RewardSummaryResponse summary = rewardService.getRewardSummary(accountId);
        return ResponseEntity.ok(summary);
    }

    @Operation(
            summary = "Redeem reward points for cash",
            description = """
                    Converts reward points to cash and credits them to the account balance.
                    
                    **Redemption rules:**
                    - Account must exist and be **ACTIVE**
                    - Minimum redemption: **10 points**
                    - Cannot redeem more than available points
                    - Rate: **1 point = ₹1** credited to account balance
                    
                    **Example:** Redeeming 50 points adds ₹50.00 to account balance.
                    """,
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Points redeemed successfully",
                    content = @Content(schema = @Schema(implementation = RewardRedemptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found (ACC-404)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = """
                    Validation or business rule failure (VAL-422):
                    - Points requested < 10 (minimum)
                    - Insufficient available points
                    - Account is not active
                    """,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{accountId}/redeem")
    public ResponseEntity<RewardRedemptionResponse> redeemRewards(
            @Parameter(description = "Account ID to redeem points for", example = "1")
            @PathVariable Long accountId,
            @Valid @RequestBody RewardRedemptionRequest request) {
        RewardRedemptionResponse response = rewardService.redeemPoints(accountId, request.getPoints());
        return ResponseEntity.ok(response);
    }
}

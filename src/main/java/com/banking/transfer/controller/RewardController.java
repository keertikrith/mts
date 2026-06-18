package com.banking.transfer.controller;

import com.banking.transfer.dto.RewardRedemptionRequest;
import com.banking.transfer.dto.RewardRedemptionResponse;
import com.banking.transfer.dto.RewardSummaryResponse;
import com.banking.transfer.service.RewardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class RewardController {

    private final RewardService rewardService;

    /**
     * GET /api/v1/rewards/{accountId}
     *
     * Returns the total reward points and full reward history for the given account.
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<RewardSummaryResponse> getRewards(@PathVariable Long accountId) {
        RewardSummaryResponse summary = rewardService.getRewardSummary(accountId);
        return ResponseEntity.ok(summary);
    }

    /**
     * POST /api/v1/rewards/{accountId}/redeem
     *
     * Redeems reward points for cash credit into the account balance.
     * 1 point = ₹1. Minimum 10 points required.
     *
     * Request body: { "points": 50 }
     */
    @PostMapping("/{accountId}/redeem")
    public ResponseEntity<RewardRedemptionResponse> redeemRewards(
            @PathVariable Long accountId,
            @Valid @RequestBody RewardRedemptionRequest request) {
        RewardRedemptionResponse response = rewardService.redeemPoints(accountId, request.getPoints());
        return ResponseEntity.ok(response);
    }
}

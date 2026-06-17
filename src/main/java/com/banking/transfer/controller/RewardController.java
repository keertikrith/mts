package com.banking.transfer.controller;

import com.banking.transfer.dto.RewardSummaryResponse;
import com.banking.transfer.service.RewardService;
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
}

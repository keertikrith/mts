package com.banking.transfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardSummaryResponse {

    private Long accountId;
    private int totalPoints;                                // All-time earned points
    private int redeemedPoints;                             // Total points redeemed so far
    private int availablePoints;                            // totalPoints - redeemedPoints
    private List<RewardLedgerResponse> history;             // Earn history
    private List<RewardRedemptionLedgerResponse> redemptionHistory; // Redeem history
}

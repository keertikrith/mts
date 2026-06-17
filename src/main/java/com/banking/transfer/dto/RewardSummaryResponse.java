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
    private int totalPoints;
    private List<RewardLedgerResponse> history;
}

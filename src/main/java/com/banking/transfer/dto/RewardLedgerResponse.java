package com.banking.transfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardLedgerResponse {

    private Long id;
    private String transactionId;
    private int pointsEarned;
    private LocalDateTime createdOn;
}
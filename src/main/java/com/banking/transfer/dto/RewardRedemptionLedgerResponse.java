package com.banking.transfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardRedemptionLedgerResponse {

    private Long id;
    private int pointsRedeemed;
    private BigDecimal amountCredited;
    private LocalDateTime redeemedOn;
}

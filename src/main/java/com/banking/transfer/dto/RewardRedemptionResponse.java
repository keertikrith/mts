package com.banking.transfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardRedemptionResponse {

    private int pointsRedeemed;
    private BigDecimal amountCredited;
    private int availablePoints;
    private BigDecimal newBalance;
    private String message;
}

package com.banking.transfer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardRedemptionRequest {

    @NotNull
    @Min(value = 10, message = "Minimum redemption is 10 points")
    private Integer points;
}

package com.banking.transfer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for redeeming reward points")
public class RewardRedemptionRequest {

    @NotNull
    @Min(value = 10, message = "Minimum redemption is 10 points")
    @Schema(description = "Number of points to redeem (minimum 10). Each point = ₹1 credited to balance.", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer points;
}

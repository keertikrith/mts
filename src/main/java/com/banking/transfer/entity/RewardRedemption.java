package com.banking.transfer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reward_redemptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private int pointsRedeemed;

    // Amount credited to account (1 pt = ₹1)
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amountCredited;

    @Column(nullable = false)
    private LocalDateTime redeemedOn;

    @PrePersist
    public void prePersist() {
        if (this.redeemedOn == null) {
            this.redeemedOn = LocalDateTime.now();
        }
    }
}

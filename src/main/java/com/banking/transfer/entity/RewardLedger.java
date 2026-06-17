package com.banking.transfer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reward_ledger")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    // The transaction that triggered this reward
    @Column(nullable = false, unique = true)
    private String transactionId;

    @Column(nullable = false)
    private int pointsEarned;

    @Column(nullable = false)
    private LocalDateTime createdOn;

    @PrePersist
    public void prePersist() {
        if (this.createdOn == null) {
            this.createdOn = LocalDateTime.now();
        }
    }
}

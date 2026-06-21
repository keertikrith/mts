package com.banking.transfer.service;

import com.banking.transfer.entity.RewardLedger;
import com.banking.transfer.entity.RewardType;
import com.banking.transfer.entity.TransactionLog;
import com.banking.transfer.entity.TransactionStatus;
import com.banking.transfer.repository.RewardLedgerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock
    private RewardLedgerRepository rewardLedgerRepository;

    @InjectMocks
    private RewardService rewardService;

    @Test
    void redeemPoints_CreatesRedeemLedgerEntry() {
        when(rewardLedgerRepository.existsByTransactionIdAndRewardType("TX123", RewardType.REDEEM))
                .thenReturn(false);

        rewardService.redeemPoints(1L, "TX123", 50);

        ArgumentCaptor<RewardLedger> ledgerCaptor = ArgumentCaptor.forClass(RewardLedger.class);
        verify(rewardLedgerRepository).save(ledgerCaptor.capture());

        RewardLedger ledger = ledgerCaptor.getValue();
        assertEquals(1L, ledger.getAccountId());
        assertEquals("TX123", ledger.getTransactionId());
        assertEquals(RewardType.REDEEM, ledger.getRewardType());
        assertEquals(-50, ledger.getPointsEarned());
    }

    @Test
    void processReward_CreatesEarnLedgerEntryWhenRedeemEntryAlreadyExists() {
        TransactionLog transactionLog = TransactionLog.builder()
                .id("TX123")
                .fromAccountId(1L)
                .toAccountId(2L)
                .amount(new BigDecimal("1000.00"))
                .status(TransactionStatus.SUCCESS)
                .build();

        when(rewardLedgerRepository.existsByTransactionIdAndRewardType("TX123", RewardType.EARN))
                .thenReturn(false);

        rewardService.processReward(transactionLog, 1L, new BigDecimal("950.00"));

        ArgumentCaptor<RewardLedger> ledgerCaptor = ArgumentCaptor.forClass(RewardLedger.class);
        verify(rewardLedgerRepository).save(ledgerCaptor.capture());

        RewardLedger ledger = ledgerCaptor.getValue();
        assertEquals(1L, ledger.getAccountId());
        assertEquals("TX123", ledger.getTransactionId());
        assertEquals(RewardType.EARN, ledger.getRewardType());
        assertEquals(9, ledger.getPointsEarned());
    }

    @Test
    void processReward_SkipsDuplicateEarnOnly() {
        TransactionLog transactionLog = TransactionLog.builder()
                .id("TX123")
                .fromAccountId(1L)
                .toAccountId(2L)
                .amount(new BigDecimal("1000.00"))
                .status(TransactionStatus.SUCCESS)
                .build();

        when(rewardLedgerRepository.existsByTransactionIdAndRewardType("TX123", RewardType.EARN))
                .thenReturn(true);

        rewardService.processReward(transactionLog, 1L, new BigDecimal("950.00"));

        verify(rewardLedgerRepository, never()).save(any(RewardLedger.class));
    }

    @Test
    void redeemPoints_SkipsDuplicateRedeemOnly() {
        when(rewardLedgerRepository.existsByTransactionIdAndRewardType("TX123", RewardType.REDEEM))
                .thenReturn(true);

        rewardService.redeemPoints(1L, "TX123", 50);

        verify(rewardLedgerRepository, never()).save(any(RewardLedger.class));
    }
}

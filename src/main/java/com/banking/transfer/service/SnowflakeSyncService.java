package com.banking.transfer.service;

import com.banking.transfer.entity.Account;
import com.banking.transfer.entity.RewardLedger;
import com.banking.transfer.entity.RewardRedemption;
import com.banking.transfer.entity.TransactionLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

/**
 * Pushes every data event to Snowflake asynchronously.
 *
 * All methods are @Async — they fire-and-forget on a background thread.
 * The main transaction always succeeds even if Snowflake is temporarily down.
 *
 * If the Snowflake Connection bean is not created (snowflake.enabled=false),
 * all methods silently do nothing.
 *
 * Tables written to (defined in snowflake/04_mirror_tables.sql):
 *   SF_ACCOUNTS            — account master data
 *   SF_TRANSACTION_LOGS    — all transfers
 *   SF_REWARD_LEDGER       — points earned per transaction
 *   SF_REWARD_REDEMPTIONS  — redemption events
 */
@Service
@Slf4j
public class SnowflakeSyncService {

    // Optional — not present when snowflake.enabled=false
    private final Optional<Connection> snowflake;

    @Autowired
    public SnowflakeSyncService(@Autowired(required = false) Connection snowflakeConnection) {
        this.snowflake = Optional.ofNullable(snowflakeConnection);
        if (snowflake.isPresent()) {
            log.info("SnowflakeSyncService: Snowflake connection active — data will be synced.");
        } else {
            log.info("SnowflakeSyncService: Snowflake disabled — sync is a no-op.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACCOUNT SYNC
    // Called after account creation so Snowflake always has current accounts.
    // Uses MERGE to handle both insert and update (account status changes).
    // ─────────────────────────────────────────────────────────────────────────

    @Async
    public void syncAccount(Account account) {
        if (snowflake.isEmpty()) return;

        String sql = """
                MERGE INTO SF_ACCOUNTS AS target
                USING (SELECT ? AS ID, ? AS USERNAME, ? AS HOLDER_NAME,
                              ? AS BALANCE, ? AS STATUS, ? AS LAST_UPDATED) AS source
                ON target.ID = source.ID
                WHEN MATCHED THEN
                    UPDATE SET BALANCE      = source.BALANCE,
                               STATUS       = source.STATUS,
                               LAST_UPDATED = source.LAST_UPDATED
                WHEN NOT MATCHED THEN
                    INSERT (ID, USERNAME, HOLDER_NAME, BALANCE, STATUS, LAST_UPDATED)
                    VALUES (source.ID, source.USERNAME, source.HOLDER_NAME,
                            source.BALANCE, source.STATUS, source.LAST_UPDATED)
                """;
        try (PreparedStatement ps = snowflake.get().prepareStatement(sql)) {
            ps.setLong  (1, account.getId());
            ps.setString(2, account.getUsername());
            ps.setString(3, account.getHolderName());
            ps.setBigDecimal(4, account.getBalance());
            ps.setString(5, account.getStatus().name());
            ps.setTimestamp(6, account.getLastUpdated() != null
                    ? Timestamp.valueOf(account.getLastUpdated())
                    : new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
            log.debug("[SF] Synced account id={}", account.getId());
        } catch (SQLException e) {
            log.error("[SF] Failed to sync account {}: {}", account.getId(), e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TRANSACTION SYNC
    // Called after every successful or failed transfer.
    // Idempotent — skips if transaction_id already exists.
    // ─────────────────────────────────────────────────────────────────────────

    @Async
    public void syncTransaction(TransactionLog tx) {
        if (snowflake.isEmpty()) return;

        String sql = """
                INSERT INTO SF_TRANSACTION_LOGS
                    (ID, FROM_ACCOUNT_ID, TO_ACCOUNT_ID, AMOUNT,
                     STATUS, FAILURE_REASON, IDEMPOTENCY_KEY, CREATED_ON)
                SELECT ?, ?, ?, ?, ?, ?, ?, ?
                WHERE NOT EXISTS (
                    SELECT 1 FROM SF_TRANSACTION_LOGS WHERE ID = ?
                )
                """;
        try (PreparedStatement ps = snowflake.get().prepareStatement(sql)) {
            ps.setString    (1, tx.getId());
            ps.setLong      (2, tx.getFromAccountId());
            ps.setLong      (3, tx.getToAccountId());
            ps.setBigDecimal(4, tx.getAmount());
            ps.setString    (5, tx.getStatus().name());
            ps.setString    (6, tx.getFailureReason());
            ps.setString    (7, tx.getIdempotencyKey());
            ps.setTimestamp (8, Timestamp.valueOf(tx.getCreatedOn()));
            ps.setString    (9, tx.getId()); // for NOT EXISTS check
            ps.executeUpdate();
            log.debug("[SF] Synced transaction id={}", tx.getId());
        } catch (SQLException e) {
            log.error("[SF] Failed to sync transaction {}: {}", tx.getId(), e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REWARD EARN SYNC
    // Called after points are awarded for a transfer.
    // ─────────────────────────────────────────────────────────────────────────

    @Async
    public void syncRewardEarned(RewardLedger ledger) {
        if (snowflake.isEmpty()) return;

        String sql = """
                INSERT INTO SF_REWARD_LEDGER
                    (ID, ACCOUNT_ID, TRANSACTION_ID, POINTS_EARNED, CREATED_ON)
                SELECT ?, ?, ?, ?, ?
                WHERE NOT EXISTS (
                    SELECT 1 FROM SF_REWARD_LEDGER WHERE TRANSACTION_ID = ?
                )
                """;
        try (PreparedStatement ps = snowflake.get().prepareStatement(sql)) {
            ps.setLong     (1, ledger.getId());
            ps.setLong     (2, ledger.getAccountId());
            ps.setString   (3, ledger.getTransactionId());
            ps.setInt      (4, ledger.getPointsEarned());
            ps.setTimestamp(5, Timestamp.valueOf(ledger.getCreatedOn()));
            ps.setString   (6, ledger.getTransactionId());
            ps.executeUpdate();
            log.debug("[SF] Synced reward earn: {} pts for account {}", ledger.getPointsEarned(), ledger.getAccountId());
        } catch (SQLException e) {
            log.error("[SF] Failed to sync reward earn: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REWARD REDEMPTION SYNC
    // Called after points are redeemed for cash.
    // ─────────────────────────────────────────────────────────────────────────

    @Async
    public void syncRewardRedemption(RewardRedemption redemption) {
        if (snowflake.isEmpty()) return;

        String sql = """
                INSERT INTO SF_REWARD_REDEMPTIONS
                    (ID, ACCOUNT_ID, POINTS_REDEEMED, AMOUNT_CREDITED, REDEEMED_ON)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = snowflake.get().prepareStatement(sql)) {
            ps.setLong      (1, redemption.getId());
            ps.setLong      (2, redemption.getAccountId());
            ps.setInt       (3, redemption.getPointsRedeemed());
            ps.setBigDecimal(4, redemption.getAmountCredited());
            ps.setTimestamp (5, Timestamp.valueOf(redemption.getRedeemedOn()));
            ps.executeUpdate();
            log.debug("[SF] Synced redemption: {} pts for account {}", redemption.getPointsRedeemed(), redemption.getAccountId());
        } catch (SQLException e) {
            log.error("[SF] Failed to sync redemption: {}", e.getMessage());
        }
    }
}

package com.banking.transfer.repository;

import com.banking.transfer.entity.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, String> {

    Optional<TransactionLog> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT t FROM TransactionLog t WHERE t.fromAccountId = ?1 OR t.toAccountId = ?1 ORDER BY t.createdOn DESC")
    List<TransactionLog> findByAccountId(Long accountId);
}

package com.rbms.renbo.repository;

import com.rbms.renbo.entity.Transactions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transactions, Long> {

    @Query("SELECT t FROM Transactions t WHERE t.owner.userID = ?1")
    List<Transactions> findByOwnerId(UUID ownerId);

    @Query("SELECT t FROM Transactions t WHERE t.renter.userID = ?1")
    List<Transactions> findByRenterId(UUID renterId);

    @Query("SELECT t FROM Transactions t WHERE t.item.ID = ?1")
    List<Transactions> findByItemId(UUID itemId);

    @Query("SELECT t FROM Transactions t WHERE t.transactionStatus = ?1")
    List<Transactions> findByTransactionStatus(String status);

    @Query("SELECT t FROM Transactions t WHERE t.paymentStatus = ?1")
    List<Transactions> findByPaymentStatus(String status);

    @Query("SELECT t FROM Transactions t WHERE t.paymentStatus = ?1 AND t.owner.userID = ?2")
    List<Transactions> findByOwnerAndPaymentStatus(String status, UUID ownerId);

    // ─── Reporting queries ───────────────────────────────────────────────────

    /**
     * Completed + paid transactions for an owner within a date range.
     * Used for earnings report — date range is matched against startDate.
     */
    @Query("SELECT t FROM Transactions t WHERE " +
            "t.owner.userID      = :ownerId AND " +
            "t.transactionStatus = 'COMPLETED' AND " +
            "t.paymentStatus     = 'PAID' AND " +
            "t.startDate        >= :from AND " +
            "t.startDate        <= :to")
    List<Transactions> findCompletedPaidByOwnerAndDateRange(
            @Param("ownerId") UUID      ownerId,
            @Param("from")    LocalDate from,
            @Param("to")      LocalDate to);

    /**
     * All transactions for an owner — used for status summary counts.
     * No date filter so it reflects lifetime totals.
     */
    @Query("SELECT t FROM Transactions t WHERE t.owner.userID = :ownerId")
    List<Transactions> findAllByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Platform-wide completed + paid transactions — used by admin report.
     */
    @Query("SELECT t FROM Transactions t WHERE " +
            "t.transactionStatus = 'COMPLETED' AND " +
            "t.paymentStatus     = 'PAID'")
    List<Transactions> findAllCompletedAndPaid();
}
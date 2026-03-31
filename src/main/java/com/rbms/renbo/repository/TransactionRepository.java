package com.rbms.renbo.repository;

import com.rbms.renbo.entity.Transactions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
}

package com.rbms.renbo.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.rbms.renbo.constant.PaymentStatusEnum;
import com.rbms.renbo.constant.TransactionStatusEnum;
import com.rbms.renbo.constant.TransactionTypeEnum;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "transactions")
@Data
public class Transactions {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "transactionID")
    private Long transactionID;

    // ─── WHO ──────────────────────────────────────────
    @ManyToOne
    @JoinColumn(name = "ownerID")
    private User owner;       // still named owner for clarity

    @ManyToOne
    @JoinColumn(name = "renterID")
    private User renter;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private Item item;

    // ─── WHAT TYPE ────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "transactionType")
    private TransactionTypeEnum transactionType; // RENT or BORROW

    // ─── WHEN ─────────────────────────────────────────
    @Column(name = "startDate")
    private LocalDate startDate;

    @Column(name = "endDate")
    private LocalDate endDate;

    @Column(name = "requestedDate")
    private LocalDateTime requestedDate; // when renter submitted

    @Column(name = "approvedDate")
    private LocalDateTime approvedDate; // when owner approved

    @Column(name = "returnedDate")
    private LocalDateTime returnedDate; // when item was returned

    // ─── STATUS ───────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "transactionStatus")
    private TransactionStatusEnum transactionStatus;
    // PENDING → APPROVED → ACTIVE → COMPLETED
    //                    ↘ CANCELLED

    // ─── MONEY ────────────────────────────────────────
    @Column(name = "dailyRate")
    private BigDecimal dailyRate;       // snapshot of item rate at time of booking

    @Column(name = "depositAmount")
    private BigDecimal depositAmount;   // snapshot of deposit at time of booking

    @Column(name = "totalDays")
    private int totalDays;         // endDate - startDate

    @Column(name = "totalAmount")
    private BigDecimal totalAmount;     // dailyRate × totalDays

    // ─── PAYMENT ──────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "paymentStatus")
    private PaymentStatusEnum paymentStatus; // UNPAID, PAID, DEPOSIT_RETURNED

    @Column(name = "paymentRef")
    private String paymentRef;     // manual reference e.g. "TNG ref #12345"

    @Column(name = "paymentDate")
    private LocalDateTime paymentDate;

    // ─── NOTES ────────────────────────────────────────
    @Column(name = "renterNote")
    private String renterNote;     // message from renter when requesting

    @Column(name = "ownerNote")
    private String ownerNote;      // reason if rejected/cancelled
}
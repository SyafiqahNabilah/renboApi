package com.rbms.renbo.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Platform-wide admin report — user counts, item counts,
 * transaction volume, and total earnings.
 */
@Data
public class AdminSystemReportDto {

    // ── Users ─────────────────────────────────────────────────────────────────
    private long totalUsers;
    private long totalOwners;
    private long totalRenters;
    private long totalAdmins;

    // ── Items ─────────────────────────────────────────────────────────────────
    private long totalItems;

    // ── Transactions ──────────────────────────────────────────────────────────
    private long totalTransactions;
    private long pendingTransactions;
    private long approvedTransactions;
    private long activeTransactions;
    private long completedTransactions;
    private long cancelledTransactions;

    // ── Money ─────────────────────────────────────────────────────────────────
    /** Sum of totalAmount for all COMPLETED + PAID transactions across the platform. */
    private BigDecimal totalPlatformEarnings;

    private LocalDateTime generatedAt;
}
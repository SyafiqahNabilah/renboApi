package com.rbms.renbo.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Owner earnings report — total income for a given date range
 * with per-item breakdown.
 * Covers only COMPLETED + PAID transactions.
 */
@Data
public class OwnerEarningsReportDto {

    private UUID   ownerId;
    private String ownerName;

    /** Inclusive date range applied to transaction startDate / endDate. */
    private LocalDate fromDate;
    private LocalDate toDate;

    /** Total earnings (sum of totalAmount) across all items in the period. */
    private BigDecimal totalEarnings;

    /** Number of COMPLETED + PAID transactions in the period. */
    private long completedTransactionCount;

    /** Per-item breakdown — sorted by totalEarnings descending. */
    private List<ItemEarningDto> itemBreakdown;

    private LocalDateTime generatedAt;
}
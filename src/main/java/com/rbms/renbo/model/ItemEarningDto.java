package com.rbms.renbo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Per-item earnings breakdown within an owner's earnings report.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemEarningDto {

    private UUID   itemId;
    private String itemName;

    /** Sum of totalAmount for all COMPLETED + PAID transactions on this item. */
    private BigDecimal totalEarnings;

    /** Number of completed transactions for this item in the period. */
    private long completedTransactionCount;

    /** Total rental days across all completed transactions for this item. */
    private int totalDays;
}
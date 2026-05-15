package com.rbms.renbo.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Owner's transaction status summary — counts of each lifecycle state.
 */
@Data
public class OwnerTransactionSummaryDto {

    private UUID   ownerId;
    private String ownerName;

    private long pending;
    private long approved;
    private long active;
    private long completed;
    private long cancelled;

    /** Sum of all status counts. */
    private long total;

    private LocalDateTime generatedAt;
}
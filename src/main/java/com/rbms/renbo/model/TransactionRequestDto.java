package com.rbms.renbo.model;

import com.rbms.renbo.constant.TransactionTypeEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class TransactionRequestDto {
    // Who
    private UUID ownerId;
    private UUID renterId;
    private UUID itemId;

    // What type
    private TransactionTypeEnum transactionType; // RENT or BORROW

    // When
    private LocalDate startDate;
    private LocalDate endDate;

    // Money (optional, can be calculated from item)
    private BigDecimal dailyRate;
    private BigDecimal depositAmount;

    // Notes
    private String renterNote;

    // Payment
    private String paymentRef;
}

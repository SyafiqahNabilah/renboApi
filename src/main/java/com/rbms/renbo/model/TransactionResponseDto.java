package com.rbms.renbo.model;

import com.rbms.renbo.constant.PaymentStatusEnum;
import com.rbms.renbo.constant.TransactionStatusEnum;
import com.rbms.renbo.constant.TransactionTypeEnum;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TransactionResponseDto {
    private Long transactionID;

    // Who
    private String ownerName;
    private String ownerEmail;
    private String renterName;
    private String renterEmail;
    private String itemName;
    private String itemDescription;

    // What type
    private TransactionTypeEnum transactionType;

    // When
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime requestedDate;
    private LocalDateTime approvedDate;
    private LocalDateTime returnedDate;

    // Status
    private TransactionStatusEnum transactionStatus;

    // Money
    private Float dailyRate;
    private Float depositAmount;
    private Integer totalDays;
    private Float totalAmount;

    // Payment
    private PaymentStatusEnum paymentStatus;
    private String paymentRef;
    private LocalDateTime paymentDate;

    // Notes
    private String renterNote;
    private String ownerNote;
}

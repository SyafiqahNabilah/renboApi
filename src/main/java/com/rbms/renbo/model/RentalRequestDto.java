package com.rbms.renbo.model;

import com.rbms.renbo.constant.TransactionTypeEnum;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class RentalRequestDto {
    private UUID itemId;
    private LocalDate startDate;
    private LocalDate endDate;
    private TransactionTypeEnum transactionType;
    private String renterNote;
}

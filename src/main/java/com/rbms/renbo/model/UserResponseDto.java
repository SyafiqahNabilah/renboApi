package com.rbms.renbo.model;

import lombok.Data;

import java.util.List;

@Data
public class UserResponseDto {
    private String fullName;
    private String email;
    private String address;
    private String role;
    private String status;
    private String joined;
    private int itemCount;
    private int transactionCount;
    private List<ItemResponseDto> items;
    private List<TransactionResponseDto> transactions;

}

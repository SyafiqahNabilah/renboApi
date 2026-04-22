package com.rbms.renbo.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ItemResponseDto {

    UUID id;
    String name;
    String description;
    String brand;
    String height;
    String width;
    String depth;
    String weight;
    String category;
    int quantity;
    String material;
    String schema;
    BigDecimal rate;
    BigDecimal deposit;
    String pickupMethod;
    String itemImage1;
    String itemImage2;
    String itemImage3;
    String availability;

}
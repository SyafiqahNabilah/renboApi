package com.rbms.renbo.model;

import lombok.Data;

@Data
public class ItemResponseDto {

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
    float rate;
    float deposit;
    String pickupMethod;
    String itemImage1;
    String itemImage2;
    String itemImage3;
    String availability;

}
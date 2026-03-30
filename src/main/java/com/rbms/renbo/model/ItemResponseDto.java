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
    
    // String itemImage_1;

    // @Column(name = "itemImage_2")
    // String itemImage_2;

    // @Column(name = "itemImage_3")
    // String itemImage_3;

    String availability;

}
package com.rbms.renbo.model;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ItemRequestDto {
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
    String type;
    BigDecimal rate;
    BigDecimal deposit;
    String pickupMethod;
    MultipartFile itemImage1;
    MultipartFile itemImage2;
    MultipartFile itemImage3;
    String availability;
    UUID ownerId;

}
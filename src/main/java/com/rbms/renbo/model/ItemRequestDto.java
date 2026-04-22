package com.rbms.renbo.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ItemRequestDto {
    @NotBlank(message = "Item name is required")
    String name;
    @NotBlank(message = "Item description is required")
    String description;
    String brand;
    String height;
    String width;
    String depth;
    String weight;
    @NotBlank(message = "Item category is required")
    String category;
    @Min(value = 1, message = "Quantity must be at least 1")
    int quantity;
    String material;
    String type;
    @DecimalMin(value = "0.0", inclusive = false, message = "Rate must be greater than 0")
    BigDecimal rate;
    BigDecimal deposit;
    String pickupMethod;
    MultipartFile itemImage1;
    MultipartFile itemImage2;
    MultipartFile itemImage3;
    String availability;
    UUID ownerId;

}
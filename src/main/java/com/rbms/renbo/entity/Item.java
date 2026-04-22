package com.rbms.renbo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;


/**
 *
 * @author Syafiqah Nabilah
 */
@Data
@Entity
@Table(name = "item")
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "Id")
    private UUID ID;
    @ManyToOne
    @JoinColumn(name = "ownerID")
    private User owner;
    @Column(name = "name")
    private String name;
    @Column(name = "description")
    private String description;
    @Column(name = "brand")
    private String brand;
    @Column(name = "height")
    private String height;
    @Column(name = "width")
    private String width;
    @Column(name = "depth")
    private String depth;
    @Column(name = "weight")
    private String weight;
    @Column(name = "category")
    private String category;
    @Column(name = "quantity")
    private int quantity;
    @Column(name = "material")
    private String material;
    @Column(name = "type")
    private String type;
    @Column(name = "rate")
    private BigDecimal rate;
    @Column(name = "deposit")
    private BigDecimal deposit;
    @Column(name = "pickupMethod")
    private String pickupMethod;
    @Column(name = "itemImage1")
    private String itemImage1;
    @Column(name = "itemImage2")
    private String itemImage2;
    @Column(name = "itemImage3")
    private String itemImage3;
    @Column(name = "availability")
    private String availability;
}

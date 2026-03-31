/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rbms.renbo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;


/**
 *
 * @author Syafiqah Nabilah
 */
@Data
@Entity
@Table(name = "item")
public class Item implements Serializable {
    @Id
    @Column(name = "Id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    UUID ID;
    
    @ManyToOne
    @JoinColumn(name = "ownerID")
    User owner;

    @Column(name = "name")
    String name;
    @Column(name = "description")
    String description;
    @Column(name = "brand")
    String brand;
    @Column(name = "height")
    String height;
    @Column(name = "width")
    String width;
    @Column(name = "depth")
    String depth;
    @Column(name = "weight")
    String weight;
    @Column(name = "category")
    String category;
    @Column(name = "quantity")
    int quantity;
    @Column(name = "material")
    String material;
    @Column(name = "type")
    String type;
    @Column(name = "rate")
    BigDecimal rate;
    @Column(name = "deposit")
    BigDecimal deposit;
    @Column(name = "pickupMethod")
    String pickupMethod;
    @Column(name = "itemImage1")
    String itemImage1;
    @Column(name = "itemImage2")
    String itemImage2;
    @Column(name = "itemImage3")
    String itemImage3;
    @Column(name = "availability")
    String availability;

    @Transient
    public String getPhotosImagePath() {
        if (itemImage1 == null) return null;
        return "/img/catalog/" + itemImage1;
    }

    public String getPhotosImagePath2() {
        if (itemImage2 == null) return null;
        return "/img/catalog/" + itemImage2;
    }

    public String getPhotosImagePath3() {
        if (itemImage3 == null) return null;
        return "/img/catalog/" + itemImage3;
    }


}

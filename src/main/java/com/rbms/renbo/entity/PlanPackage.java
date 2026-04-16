package com.rbms.renbo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "planPackage")
public class PlanPackage {
    @Id
    @Column(name = "Id")
    private UUID id;
    private String name;
    private String description;
    private Double price;

}

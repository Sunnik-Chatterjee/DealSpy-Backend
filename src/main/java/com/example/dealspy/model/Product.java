package com.example.dealspy.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product")
@NoArgsConstructor
@Getter
@Setter
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer pid;

    @Column(unique = true, nullable = false)
    private String name;

    private Double currentPrice;
    private Double lastLowestPrice;
    private Boolean isPriceDropped;
}

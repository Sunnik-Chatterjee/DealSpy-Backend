package com.example.dealspy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product")
@NoArgsConstructor
@AllArgsConstructor
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
    private String imageUrl;
    @Column(name = "`desc`")
    private String desc;
    private String deepLink;
}

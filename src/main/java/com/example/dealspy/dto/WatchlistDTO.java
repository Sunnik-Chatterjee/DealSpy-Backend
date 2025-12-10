package com.example.dealspy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistDTO {
    private String productName;
    private String brand;
    private String platform;   // was platformName
    private String imageUrl;
    private String deepLink;
    private Double price;      // was currentPrice
}

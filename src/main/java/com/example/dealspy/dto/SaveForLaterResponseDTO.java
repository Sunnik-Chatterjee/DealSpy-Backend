package com.example.dealspy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveForLaterResponseDTO {
    private String productName;
    private String brand;
    private String platform;       // renamed from platformName
    private String imageUrl;
    private String deepLink;
    private Double price;          // renamed from currentPrice
    private Double lastKnownPrice;
}

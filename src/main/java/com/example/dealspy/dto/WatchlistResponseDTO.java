package com.example.dealspy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistResponseDTO {
    private String productName;
    private String brand;
    private String platformName;
    private String imageUrl;
    private String deepLink;
    private Double currentPrice;
    private Double lastKnownPrice;
}

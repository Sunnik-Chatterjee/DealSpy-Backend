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
    private LocalDate watchEndDate;
    private String imageUrl;
    private String desc;
}
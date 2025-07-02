package com.example.dealspy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistDTO {
    private String productName;       // mandatory for add + delete
    private LocalDate watchEndDate;   // used only for add/get
}
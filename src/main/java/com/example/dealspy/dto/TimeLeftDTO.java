package com.example.dealspy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeLeftDTO {
    private long days;
    private int hours;
    private int minutes;
    private int seconds;
}

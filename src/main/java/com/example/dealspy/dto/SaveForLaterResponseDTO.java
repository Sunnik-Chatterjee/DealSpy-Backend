package com.example.dealspy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveForLaterResponseDTO {
    private String productName;
    private String imageUrl;
    private String desc;
    private String deepLink;
}

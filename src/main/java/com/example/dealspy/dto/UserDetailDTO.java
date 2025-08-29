package com.example.dealspy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailDTO {
    private List<WatchlistDTO> watchList;
    private List<SaveForLaterDTO> saveForLater;
}

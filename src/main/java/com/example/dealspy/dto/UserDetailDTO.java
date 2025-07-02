package com.example.dealspy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class UserDetailDTO {
    private List<WatchlistDTO> watchList;
    private List<SaveForLaterDTO> saveForLater;
}

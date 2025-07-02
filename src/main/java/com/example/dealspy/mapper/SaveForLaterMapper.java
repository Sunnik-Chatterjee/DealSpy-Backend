package com.example.dealspy.mapper;

import com.example.dealspy.model.SaveForLater;
import com.example.dealspy.dto.SaveForLaterDTO;

public class SaveForLaterMapper {

    public static SaveForLaterDTO toDTO(SaveForLater saveForLater) {
        return new SaveForLaterDTO(
                saveForLater.getProduct().getName()
        );
    }
}

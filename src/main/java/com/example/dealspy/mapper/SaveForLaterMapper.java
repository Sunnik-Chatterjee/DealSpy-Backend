package com.example.dealspy.mapper;

import com.example.dealspy.dto.SaveForLaterDTO;
import com.example.dealspy.dto.SaveForLaterResponseDTO;
import com.example.dealspy.model.SaveForLater;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;  // ✅ Add this import

@Mapper(componentModel = "spring")
public interface SaveForLaterMapper {

    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.imageUrl", target = "imageUrl")
    @Mapping(source = "product.imageUrl", target = "imageUrl")
    @Mapping(source = "product.deepLink",target ="deepLink")
    SaveForLaterDTO toDTO(SaveForLater saveForLater);

    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.imageUrl", target = "imageUrl")
    @Mapping(source = "product.deepLink",target ="deepLink")
    SaveForLaterResponseDTO toResponseDTO(SaveForLater saveForLater);

    List<SaveForLaterDTO> toDTOList(List<SaveForLater> saveForLaterList);
    List<SaveForLaterResponseDTO> toResponseDTOList(List<SaveForLater> saveForLaterList);
}

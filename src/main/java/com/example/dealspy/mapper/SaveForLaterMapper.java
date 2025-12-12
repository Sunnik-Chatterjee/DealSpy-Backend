package com.example.dealspy.mapper;

import com.example.dealspy.dto.SaveForLaterDTO;
import com.example.dealspy.dto.SaveForLaterResponseDTO;
import com.example.dealspy.model.SaveForLater;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SaveForLaterMapper {

    @Mapping(source = "product.name",         target = "productName")
    @Mapping(source = "product.brand",        target = "brand")
    @Mapping(source = "product.platform",     target = "platform")
    @Mapping(source = "product.imageUrl",     target = "imageUrl")
    @Mapping(source = "product.deepLink",     target = "deepLink")
    @Mapping(source = "product.currentPrice", target = "price")
    SaveForLaterDTO toDTO(SaveForLater saveForLater);

    @Mapping(source = "product.name",           target = "productName")
    @Mapping(source = "product.brand",          target = "brand")
    @Mapping(source = "product.platform",       target = "platform")
    @Mapping(source = "product.imageUrl",       target = "imageUrl")
    @Mapping(source = "product.deepLink",       target = "deepLink")
    @Mapping(source = "product.currentPrice",   target = "price")
    @Mapping(source = "product.lastLowestPrice", target = "lastKnownPrice")
    SaveForLaterResponseDTO toResponseDTO(SaveForLater saveForLater);

    List<SaveForLaterDTO> toDTOList(List<SaveForLater> saveForLaterList);

    List<SaveForLaterResponseDTO> toResponseDTOList(List<SaveForLater> saveForLaterList);
}

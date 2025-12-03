package com.example.dealspy.mapper;

import com.example.dealspy.dto.WatchlistDTO;
import com.example.dealspy.dto.WatchlistResponseDTO;
import com.example.dealspy.model.Watchlist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WatchlistMapper {

    @Mapping(source = "product.name",           target = "productName")
    @Mapping(source = "product.brand",          target = "brand")
    @Mapping(source = "product.platform",       target = "platformName")
    @Mapping(source = "product.imageUrl",       target = "imageUrl")
    @Mapping(source = "product.deepLink",       target = "deepLink")
    @Mapping(source = "product.currentPrice",   target = "currentPrice")
    WatchlistDTO toDTO(Watchlist watchlist);

    @Mapping(source = "product.name",           target = "productName")
    @Mapping(source = "product.brand",          target = "brand")
    @Mapping(source = "product.platform",       target = "platformName")
    @Mapping(source = "product.imageUrl",       target = "imageUrl")
    @Mapping(source = "product.deepLink",       target = "deepLink")
    @Mapping(source = "product.currentPrice",   target = "currentPrice")
    @Mapping(source = "product.lastLowestPrice", target = "lastKnownPrice")
    WatchlistResponseDTO toResponseDTO(Watchlist watchlist);

    List<WatchlistDTO> toDTOList(List<Watchlist> watchlists);

    List<WatchlistResponseDTO> toResponseDTOList(List<Watchlist> watchlists);
}

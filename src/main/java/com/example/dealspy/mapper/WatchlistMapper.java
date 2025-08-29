package com.example.dealspy.mapper;

import com.example.dealspy.dto.TimeLeftDTO;
import com.example.dealspy.dto.WatchlistDTO;
import com.example.dealspy.dto.WatchlistResponseDTO;
import com.example.dealspy.model.Watchlist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;  // ✅ Add this import

@Mapper(componentModel = "spring")
public interface WatchlistMapper {

    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.imageUrl", target = "imageUrl")
    @Mapping(source = "product.desc", target = "desc")
    @Mapping(source = "watchEndDate", target = "watchEndDate")
    WatchlistDTO toDTO(Watchlist watchlist);

    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.imageUrl", target = "imageUrl")
    @Mapping(source = "product.desc", target = "desc")
    @Mapping(source = "product.deepLink", target = "deepLink")
    @Mapping(target = "timeLeft", expression = "java(calculateTimeLeft(watchlist.getWatchEndDate()))")
    WatchlistResponseDTO toResponseDTO(Watchlist watchlist);

    List<WatchlistDTO> toDTOList(List<Watchlist> watchlists);
    List<WatchlistResponseDTO> toResponseDTOList(List<Watchlist> watchlists);


    default TimeLeftDTO calculateTimeLeft(LocalDate watchEndDate) {
        if (watchEndDate == null) {
            return new TimeLeftDTO(0, 0, 0, 0);
        }

        LocalDateTime endDateTime = watchEndDate.atTime(23, 59, 59);
        Duration duration = Duration.between(LocalDateTime.now(), endDateTime);

        if (duration.isNegative()) {
            return new TimeLeftDTO(0, 0, 0, 0);
        }

        long totalSeconds = duration.getSeconds();
        long days = totalSeconds / (24 * 3600);
        long hours = (totalSeconds % (24 * 3600)) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return new TimeLeftDTO(days, (int) hours, (int) minutes, (int) seconds);
    }
}

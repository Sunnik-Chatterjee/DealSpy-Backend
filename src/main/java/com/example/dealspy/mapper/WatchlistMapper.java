package com.example.dealspy.mapper;

import com.example.dealspy.dto.TimeLeftDTO;
import com.example.dealspy.dto.WatchlistResponseDTO;
import com.example.dealspy.model.Watchlist;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class WatchlistMapper {

    public static WatchlistResponseDTO toResponseDTO(Watchlist watchlist) {
        TimeLeftDTO timeLeft = calculateTimeLeft(watchlist.getWatchEndDate());
        return new WatchlistResponseDTO(
                watchlist.getProduct().getName(),
                timeLeft
        );
    }

    private static TimeLeftDTO calculateTimeLeft(LocalDate watchEndDate) {
        // Assuming end of the day for LocalDate
        LocalDateTime endDateTime = watchEndDate.atTime(23, 59, 59);
        Duration duration = Duration.between(LocalDateTime.now(), endDateTime);

        if (duration.isNegative()) {
            // Already expired
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


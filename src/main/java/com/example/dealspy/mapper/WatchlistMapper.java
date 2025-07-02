package com.example.dealspy.mapper;

import com.example.dealspy.model.Watchlist;
import com.example.dealspy.dto.WatchlistDTO;

public class WatchlistMapper {

    public static WatchlistDTO toDTO(Watchlist watchlist) {
        return new WatchlistDTO(
                watchlist.getProduct().getName(),
                watchlist.getWatchEndDate()
        );
    }

}

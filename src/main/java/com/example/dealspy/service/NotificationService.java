package com.example.dealspy.service;

import com.example.dealspy.model.Product;
import com.example.dealspy.model.User;
import com.example.dealspy.model.Watchlist;
import com.example.dealspy.repo.WatchListRepo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private WatchListRepo watchListRepo;

    @Autowired
    private FCMService fcmService;

    public void notifyUsersForProductPriceDrop(Product product) {
        List<User> users = watchListRepo.findUsersByProductId(product.getPid());

        for (User user : users) {
            String fcmToken = user.getFcmToken();
            if (fcmToken != null) {
                fcmService.sendNotificationToToken(
                        fcmToken,
                        "Price Dropped!",
                        "Price of " + product.getName() + " has dropped to ₹" + product.getCurrentPrice()
                );
            }
        }
    }
}

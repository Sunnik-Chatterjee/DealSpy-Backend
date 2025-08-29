package com.example.dealspy.service;

import com.example.dealspy.model.Product;
import com.example.dealspy.model.User;
import com.example.dealspy.repo.WatchListRepo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class NotificationService {

    private final WatchListRepo watchListRepo;  // ✅ Constructor injection
    private final FCMService fcmService;

    public NotificationService(WatchListRepo watchListRepo, FCMService fcmService) {
        this.watchListRepo = watchListRepo;
        this.fcmService = fcmService;
    }

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

    // ✅ ADD THIS METHOD - ProductService calls this (using Double)
    @Async
    public CompletableFuture<Void> notifyPriceDropAsync(Integer productId, String productName, Double newPrice) {
        log.info("Processing price drop notification for product: {} (ID: {})", productName, productId);

        try {
            List<User> users = watchListRepo.findUsersByProductId(productId);

            if (users.isEmpty()) {
                log.info("No users watching product: {}", productName);
                return CompletableFuture.completedFuture(null);
            }

            String title = "🎉 Price Drop Alert!";
            String body = String.format("Price of %s has dropped to ₹%.2f", productName, newPrice);

            for (User user : users) {
                String fcmToken = user.getFcmToken();
                if (fcmToken != null && !fcmToken.trim().isEmpty()) {
                    fcmService.sendNotificationToTokenAsync(fcmToken, title, body);
                    log.info("Sent notification to user: {}", user.getUid());
                }
            }

            log.info("Price drop notification sent to {} users for product: {}", users.size(), productName);

        } catch (Exception e) {
            log.error("Error in notifyPriceDropAsync for product {}: {}", productName, e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }
}

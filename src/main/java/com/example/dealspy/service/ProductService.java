package com.example.dealspy.service;

import com.example.dealspy.model.Product;
import com.example.dealspy.repo.ProductRepo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private NotificationService notificationService;

    public void updateAllProductPrices() {
        List<Product> products = productRepo.findAll();

        for (Product product : products) {
            String geminiResponse = geminiService.getCurrentLowestPrice(product.getName());
            double currentPrice = geminiService.extractPrice(geminiResponse);

            product.setCurrentPrice(currentPrice);

            if (currentPrice < product.getLastLowestPrice()) {
                product.setLastLowestPrice(currentPrice);
                product.setIsPriceDropped(true);

                notificationService.notifyUsersForProductPriceDrop(product);
            } else {
                product.setIsPriceDropped(false);
            }

            productRepo.save(product);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void scheduledPriceUpdate() {
        updateAllProductPrices();
    }
}


package com.example.dealspy.service;

import com.example.dealspy.model.Product;
import com.example.dealspy.repo.ProductRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepo productRepo;
    private final NotificationService notificationService;
    private final WebClient.Builder webClientBuilder;

    @Value("${dealspy.fastapi.base-url}")
    private String fastApiBaseUrl;


    public Optional<Product> getProductByName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            return Optional.empty();
        }
        return productRepo.findByName(productName.trim());
    }

    public List<Product> getAllProducts() {
        return productRepo.findAll();
    }

    public boolean productExistsByName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            return false;
        }
        return productRepo.findByName(productName.trim()).isPresent();
    }

    @Transactional
    public Product findOrCreateProduct(
            String productName,
            String brand,
            String platformName,
            String imageUrl,
            String deepLink,
            Double currentPrice,
            Double lastKnownPrice
    ) {
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty");
        }

        String trimmedName = productName.trim();
        Optional<Product> existingOpt = productRepo.findByName(trimmedName);

        if (existingOpt.isPresent()) {
            Product existing = existingOpt.get();

            if (brand != null && !brand.trim().isEmpty()) {
                existing.setBrand(brand.trim());
            }
            if (platformName != null && !platformName.trim().isEmpty()) {
                existing.setPlatform(platformName.trim());
            }
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                existing.setImageUrl(imageUrl.trim());
            }
            if (deepLink != null && !deepLink.trim().isEmpty()) {
                existing.setDeepLink(deepLink.trim());
            }
            if (currentPrice != null) {
                existing.setCurrentPrice(currentPrice);
            }
            if (lastKnownPrice != null) {
                existing.setLastLowestPrice(lastKnownPrice);
            }

            if (existing.getIsPriceDropped() == null) {
                existing.setIsPriceDropped(false);
            }

            log.debug("Updated existing product from client data: {}", trimmedName);
            return productRepo.save(existing);
        }

        Product p = new Product();
        p.setName(trimmedName);
        p.setBrand(brand != null ? brand.trim() : null);
        p.setPlatform(platformName != null ? platformName.trim() : null);
        p.setImageUrl(imageUrl != null ? imageUrl.trim() : null);
        p.setDeepLink(deepLink != null ? deepLink.trim() : null);

        p.setCurrentPrice(currentPrice);
        if (lastKnownPrice != null) {
            p.setLastLowestPrice(lastKnownPrice);
        } else {
            p.setLastLowestPrice(currentPrice);
        }

        p.setIsPriceDropped(false);

        log.info("Creating new product from client data: {}", trimmedName);
        return productRepo.save(p);
    }

    public void sendDropNotifications() {
        List<Product> allProducts = productRepo.findAll();

        List<Product> dropped = allProducts.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsPriceDropped()))
                .filter(p -> p.getCurrentPrice() != null)
                .toList();

        log.info("Sending price drop notifications for {} products", dropped.size());

        for (Product product : dropped) {
            try {
                notificationService.notifyPriceDropAsync(
                        product.getPid(),
                        product.getName(),
                        product.getCurrentPrice()
                );
            } catch (Exception e) {
                log.error(
                        "Failed to send notification for product {} (ID={}): {}",
                        product.getName(),
                        product.getPid(),
                        e.getMessage()
                );
            }
        }
    }

    /**
     * Scheduled job:
     *  1. Calls FastAPI endpoint to refresh product prices in DB.
     *  2. After FastAPI finishes, reads DB and sends price-drop notifications.
     *
     * FastAPI should expose something like: POST /v1/update/prices
     */
    @Scheduled(fixedRateString = "${dealspy.price-update-ms:14400000}")
    public void scheduledPriceUpdate() {
        WebClient client = webClientBuilder.baseUrl(fastApiBaseUrl).build();

        try {
            log.info("Triggering FastAPI price update...");

            client.post()
                    .uri("/v1/update/prices")
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            log.info("FastAPI price update completed. Now sending price drop notifications...");
            sendDropNotifications();

        } catch (Exception e) {
            log.error("Error during scheduled price update / FastAPI call: {}", e.getMessage(), e);
        }
    }

}

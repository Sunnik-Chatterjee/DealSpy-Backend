package com.example.dealspy.controller;

import com.example.dealspy.model.Product;
import com.example.dealspy.service.ProductService;
import com.example.dealspy.service.NotificationService;
import com.example.dealspy.repo.ProductRepo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ProductRepo productRepo;

    @PostMapping("/update-prices")
    public ResponseEntity<String> updatePrices() {
        productService.updateAllProductPrices();
        return ResponseEntity.ok("Prices updated and notifications sent.");
    }

    @PostMapping("/manual-price-update/{productId}/{newPrice}")
    public ResponseEntity<String> manualPriceUpdate(
            @PathVariable Integer productId,
            @PathVariable Double newPrice) {

        try {
            productService.updateProductPrice(productId, newPrice);
            return ResponseEntity.ok("Price updated to ₹" + newPrice + " and notifications sent!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/test-notification/{productId}")
    public ResponseEntity<String> testNotification(@PathVariable Integer productId) {
        try {
            Optional<Product> productOpt = productRepo.findById(productId);
            if (productOpt.isPresent()) {
                Product product = productOpt.get();

                notificationService.notifyPriceDropAsync(
                        product.getPid(),
                        product.getName(),
                        product.getCurrentPrice()
                );

                return ResponseEntity.ok("Notification triggered for: " + product.getName());
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProduct(@PathVariable Integer productId) {
        Optional<Product> product = productRepo.findById(productId);
        return product.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/list")
    public ResponseEntity<?> listProducts() {
        return ResponseEntity.ok(productRepo.findAll());
    }
}

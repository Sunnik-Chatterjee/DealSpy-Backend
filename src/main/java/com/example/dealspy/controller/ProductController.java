package com.example.dealspy.controller;

import com.example.dealspy.service.ProductService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping("/update-prices")
    public ResponseEntity<String> updatePrices() {
        productService.updateAllProductPrices();
        return ResponseEntity.ok("Prices updated and notifications sent.");
    }
}

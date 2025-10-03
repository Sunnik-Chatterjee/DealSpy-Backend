package com.example.dealspy.service;

import com.example.dealspy.model.Product;
import com.example.dealspy.repo.ProductRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class ProductService {

    private final ProductRepo productRepo;
    private final GeminiService geminiService;
    private final NotificationService notificationService;
    private final ExecutorService executorService;

    public ProductService(ProductRepo productRepo,
                          GeminiService geminiService,
                          NotificationService notificationService) {
        this.productRepo = productRepo;
        this.geminiService = geminiService;
        this.notificationService = notificationService;
        this.executorService = Executors.newFixedThreadPool(5);
    }

    // ===== BASIC CRUD OPERATIONS =====

    /**
     * Find or create a product by name
     * @param productName Name of the product
     * @return Optional containing the product
     */
    public Optional<Product> findOrCreateProduct(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            log.warn("Product name cannot be null or empty");
            return Optional.empty();
        }

        return productRepo.findByName(productName.trim())
                .or(() -> {
                    log.info("Creating new product: {}", productName);
                    Product product = new Product();
                    product.setName(productName.trim());
                    product.setIsPriceDropped(false);
                    return Optional.of(productRepo.save(product));
                });
    }

    /**
     * Get product by ID
     * @param productId Product ID
     * @return Optional containing the product
     */
    public Optional<Product> getProductById(Integer productId) {
        if (productId == null || productId <= 0) {
            log.warn("Invalid product ID: {}", productId);
            return Optional.empty();
        }
        return productRepo.findById(productId);
    }

    /**
     * Get product by name
     * @param productName Product name
     * @return Optional containing the product
     */
    public Optional<Product> getProductByName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            return Optional.empty();
        }
        return productRepo.findByName(productName.trim());
    }

    /**
     * Get all products
     * @return List of all products
     */
    public List<Product> getAllProducts() {
        return productRepo.findAll();
    }

    /**
     * Get products that have price drops
     * @return List of products with price drops
     */
    public List<Product> getProductsWithPriceDrop() {
        return productRepo.findAll().stream()
                .filter(product -> Boolean.TRUE.equals(product.getIsPriceDropped()))
                .toList();
    }

    /**
     * Save a product
     * @param product Product to save
     * @return Saved product
     */
    @Transactional
    public Product saveProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }

        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty");
        }

        product.setName(product.getName().trim());
        return productRepo.save(product);
    }

    // ===== PRICE UPDATE OPERATIONS =====

    /**
     * Update price for a specific product by ID
     * @param productId Product ID
     */
    @Transactional
    public void updateProductPriceById(Integer productId) {
        if (productId == null || productId <= 0) {
            log.warn("Invalid product ID provided: {}", productId);
            return;
        }

        productRepo.findById(productId).ifPresentOrElse(
                this::updateSingleProductPrice,
                () -> log.warn("Product with ID {} not found", productId)
        );
    }

    /**
     * Update price for a specific product by name
     * @param productName Product name
     */
    @Transactional
    public void updateProductPriceByName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            log.warn("Invalid product name provided: {}", productName);
            return;
        }

        productRepo.findByName(productName.trim()).ifPresentOrElse(
                this::updateSingleProductPrice,
                () -> log.warn("Product with name '{}' not found", productName)
        );
    }

    /**
     * Update price for multiple products asynchronously
     * @param productIds List of product IDs
     */
    public CompletableFuture<Void> updateMultipleProductPricesAsync(List<Integer> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            log.info("Starting bulk price update for {} products", productIds.size());

            for (int i = 0; i < productIds.size(); i++) {
                Integer productId = productIds.get(i);
                try {
                    log.info("Updating product {}/{}: ID {}", i + 1, productIds.size(), productId);
                    updateProductPriceById(productId);

                    // Add delay between requests to avoid rate limiting
                    if (i < productIds.size() - 1) {
                        Thread.sleep(2000); // 2 second delay
                    }

                } catch (InterruptedException e) {
                    log.error("Thread interrupted during bulk price update");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Failed to update price for product ID {}: {}", productId, e.getMessage());
                }
            }

            log.info("Completed bulk price update for {} products", productIds.size());
        }, executorService);
    }

    /**
     * Update all products prices with rate limiting
     */
    @Transactional
    public void updateAllProductPrices() {
        List<Product> products = productRepo.findAll();

        if (products.isEmpty()) {
            log.info("No products found to update");
            return;
        }

        log.info("Starting price update for {} products", products.size());

        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            try {
                log.info("Updating product {}/{}: {}", i + 1, products.size(), product.getName());
                updateSingleProductPrice(product);

                // Add delay between requests to avoid rate limiting
                if (i < products.size() - 1) {
                    Thread.sleep(2000); // 2 second delay between requests
                }

            } catch (InterruptedException e) {
                log.error("Thread interrupted during price update");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Failed to update price for product '{}': {}", product.getName(), e.getMessage());
            }
        }

        log.info("Completed price update for {} products", products.size());
    }

    /**
     * Update a single product's price using Gemini API
     * @param product Product to update
     */
    private void updateSingleProductPrice(Product product) {
        if (product == null || product.getName() == null || product.getName().trim().isEmpty()) {
            log.warn("Invalid product provided for price update");
            return;
        }

        try {
            log.debug("Fetching price for product: {}", product.getName());

            // Call Gemini API to get current price
            String geminiResponse = geminiService.getCurrentLowestPrice(product.getName());
            Double currentPrice = geminiService.extractPrice(geminiResponse);

            if (currentPrice == null || currentPrice <= 0) {
                log.warn("No valid price found for product: {}", product.getName());
                return;
            }

            Double oldPrice = product.getCurrentPrice();
            product.setCurrentPrice(currentPrice);

            // Check for price drop
            boolean priceDropped = false;
            if (oldPrice != null && currentPrice < oldPrice) {
                // Update last lowest price if this is the new lowest
                if (product.getLastLowestPrice() == null || currentPrice < product.getLastLowestPrice()) {
                    product.setLastLowestPrice(currentPrice);
                }

                product.setIsPriceDropped(true);
                priceDropped = true;

                log.info("Price drop detected for '{}': ₹{} -> ₹{}",
                        product.getName(), oldPrice, currentPrice);
            } else {
                product.setIsPriceDropped(false);

                if (oldPrice != null) {
                    log.debug("Price updated for '{}': ₹{} -> ₹{}",
                            product.getName(), oldPrice, currentPrice);
                } else {
                    log.info("Initial price set for '{}': ₹{}",
                            product.getName(), currentPrice);
                }
            }

            // Save the updated product
            productRepo.save(product);

            // Send notifications if price dropped
            if (priceDropped) {
                notificationService.notifyPriceDropAsync(
                        product.getPid(),
                        product.getName(),
                        currentPrice
                );
            }

        } catch (Exception e) {
            log.error("Failed to update price for product '{}': {}", product.getName(), e.getMessage(), e);
        }
    }

    /**
     * Manual price update with custom price
     * @param productId Product ID
     * @param newPrice New price to set
     */
    @Transactional
    public void updateProductPrice(Integer productId, Double newPrice) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Invalid product ID");
        }

        if (newPrice == null || newPrice < 0) {
            throw new IllegalArgumentException("Invalid price value");
        }

        productRepo.findById(productId).ifPresentOrElse(product -> {
            Double oldPrice = product.getCurrentPrice();
            product.setCurrentPrice(newPrice);

            // Check if price dropped
            boolean priceDropped = false;
            if (oldPrice != null && newPrice < oldPrice) {
                priceDropped = true;
                product.setIsPriceDropped(true);

                // Update last lowest price if this is lower
                if (product.getLastLowestPrice() == null || newPrice < product.getLastLowestPrice()) {
                    product.setLastLowestPrice(newPrice);
                }
            } else {
                product.setIsPriceDropped(false);
            }

            productRepo.save(product);
            log.info("Manually updated price for product {}: {} -> {}",
                    product.getName(), oldPrice, newPrice);

            // Trigger async notifications if price dropped
            if (priceDropped) {
                notificationService.notifyPriceDropAsync(productId, product.getName(), newPrice);
            }
        }, () -> {
            throw new RuntimeException("Product not found with ID: " + productId);
        });
    }

    // ===== SCHEDULED TASKS =====

    /**
     * Scheduled task to update all product prices every 30 minutes
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes
    public void scheduledPriceUpdate() {
        log.info("Starting scheduled price update");
        try {
            updateAllProductPrices();
        } catch (Exception e) {
            log.error("Error during scheduled price update: {}", e.getMessage(), e);
        }
    }

    // ===== STATISTICS AND ANALYTICS =====

    /**
     * Get product statistics
     * @return Map containing various statistics
     */
    public Map<String, Object> getProductStatistics() {
        List<Product> allProducts = productRepo.findAll();

        long totalProducts = allProducts.size();
        long productsWithPrices = allProducts.stream()
                .filter(p -> p.getCurrentPrice() != null && p.getCurrentPrice() > 0)
                .count();
        long productsWithPriceDrops = allProducts.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsPriceDropped()))
                .count();

        return Map.of(
                "totalProducts", totalProducts,
                "productsWithPrices", productsWithPrices,
                "productsWithPriceDrops", productsWithPriceDrops,
                "percentageWithPrices", totalProducts > 0 ? (double) productsWithPrices / totalProducts * 100 : 0
        );
    }

    /**
     * Delete a product by ID
     * @param productId Product ID
     */
    @Transactional
    public void deleteProduct(Integer productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Invalid product ID");
        }

        if (productRepo.existsById(productId)) {
            productRepo.deleteById(productId);
            log.info("Deleted product with ID: {}", productId);
        } else {
            throw new RuntimeException("Product not found with ID: " + productId);
        }
    }

    /**
     * Check if a product exists by name
     * @param productName Product name
     * @return true if product exists, false otherwise
     */
    public boolean productExistsByName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            return false;
        }
        return productRepo.findByName(productName.trim()).isPresent();
    }

    public Product findOrCreateProduct(String productName, String imageUrl, String desc) {
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty");
        }

        // Try to find existing product
        Optional<Product> existingProduct = productRepo.findByName(productName.trim());

        if (existingProduct.isPresent()) {
            log.debug("Found existing product: {}", productName);
            return existingProduct.get();
        }

        // Create new product with all required fields
        log.info("Creating new product with details: {}", productName);
        Product newProduct = new Product();
        newProduct.setName(productName.trim());
        newProduct.setImageUrl(imageUrl);
        newProduct.setDesc(desc);

        // Parse and set product details from description
        parseAndSetProductDetails(newProduct, desc);

        return productRepo.save(newProduct);
    }

    /**
     * Parse description to extract product details
     * @param product Product to update
     * @param desc Description string to parse
     */
    private void parseAndSetProductDetails(Product product, String desc) {
        try {
            if (desc != null && !desc.trim().isEmpty()) {
                // Parse: "Platform - ₹Price - ProductName - DeepLink"
                String[] parts = desc.split(" - ");

                if (parts.length >= 2) {
                    // Extract price (remove currency symbols and commas)
                    String priceStr = parts[1].replaceAll("[₹,\\s]", "");
                    try {
                        double price = Double.parseDouble(priceStr);
                        product.setCurrentPrice(price);  // ✅ Always set current price
                        log.debug("Extracted price for {}: ₹{}", product.getName(), price);
                    } catch (NumberFormatException e) {
                        product.setCurrentPrice(0.0);    // ✅ Fallback price
                        log.warn("Could not parse price from: {}, setting to 0.0", parts[1]);
                    }
                } else {
                    product.setCurrentPrice(0.0);        // ✅ Default price
                }
            } else {
                // No description provided
                product.setCurrentPrice(0.0);            // ✅ Required field
            }

            // ✅ Set these as null initially - will be updated by scheduled job
            product.setLastLowestPrice(null);            // ✅ Null until first update
            product.setDeepLink(null);                   // ✅ Null initially (will be extracted later)
            product.setIsPriceDropped(null);             // ✅ Null until first comparison

            log.debug("Product details set for: {} with initial price: ₹{}",
                    product.getName(), product.getCurrentPrice());

        } catch (Exception e) {
            log.error("Error parsing product details for: {}", product.getName(), e);
            // Set safe defaults if parsing fails
            product.setCurrentPrice(0.0);                // ✅ Only current price required
            product.setLastLowestPrice(null);            // ✅ Null initially
            product.setDeepLink(null);                   // ✅ Null initially
            product.setIsPriceDropped(null);             // ✅ Null initially
        }
    }
}

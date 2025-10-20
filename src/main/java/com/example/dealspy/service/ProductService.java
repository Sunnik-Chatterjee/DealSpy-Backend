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

    public Optional<Product> getProductByName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            return Optional.empty();
        }
        return productRepo.findByName(productName.trim());
    }

    public List<Product> getProductsWithPriceDrop() {
        return productRepo.findAll().stream()
                .filter(product -> Boolean.TRUE.equals(product.getIsPriceDropped()))
                .toList();
    }

    @Transactional
    public void updateAllProductPrices() {
        log.info("🚀 Starting comprehensive price update using GeminiService...");

        try {
            geminiService.updateAllProductPricesAndDeepLinks();
            sendPriceDropNotifications();

        } catch (Exception e) {
            log.error("Failed to update all product prices: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void updateProductPriceById(Integer productId) {
        if (productId == null || productId <= 0) {
            log.warn("Invalid product ID provided: {}", productId);
            return;
        }

        productRepo.findById(productId).ifPresentOrElse(
                this::updateSingleProductWithGemini,
                () -> log.warn("Product with ID {} not found", productId)
        );
    }

    private void updateSingleProductWithGemini(Product product) {
        if (product == null || product.getName() == null || product.getName().trim().isEmpty()) {
            log.warn("Invalid product provided for price update");
            return;
        }

        try {
            log.debug("Updating price for product: {}", product.getName());

            Double oldPrice = product.getCurrentPrice();
            String oldDeepLink = product.getDeepLink();

            GeminiService.PriceSearchResult result = performSingleProductSearch(product.getName());

            if (result != null && result.isSuccess()) {
                Double newPrice = result.getLowestPrice();
                String newDeepLink = result.getDeepLink();

                product.setCurrentPrice(newPrice);
                product.setDeepLink(newDeepLink);

                boolean priceDropped = false;
                if (oldPrice != null && newPrice < oldPrice) {

                    if (product.getLastLowestPrice() == null || newPrice < product.getLastLowestPrice()) {
                        product.setLastLowestPrice(newPrice);
                    }

                    product.setIsPriceDropped(true);
                    priceDropped = true;

                    log.info("💰 Price drop detected for '{}': ₹{} -> ₹{} from {}",
                            product.getName(), oldPrice, newPrice, result.getPlatform());
                } else {
                    product.setIsPriceDropped(false);

                    if (oldPrice != null) {
                        log.debug("Price updated for '{}': ₹{} -> ₹{} from {}",
                                product.getName(), oldPrice, newPrice, result.getPlatform());
                    } else {
                        log.info("Initial price set for '{}': ₹{} from {}",
                                product.getName(), newPrice, result.getPlatform());
                    }
                }

                productRepo.save(product);

                if (priceDropped) {
                    notificationService.notifyPriceDropAsync(
                            product.getPid(),
                            product.getName(),
                            newPrice
                    );
                }

            } else {
                log.warn("No valid price found for product: {}", product.getName());
            }

        } catch (Exception e) {
            log.error("Failed to update price for product '{}': {}", product.getName(), e.getMessage(), e);
        }
    }

    private GeminiService.PriceSearchResult performSingleProductSearch(String productName) {
        try {
            Product tempProduct = new Product();
            tempProduct.setName(productName);

            Double originalPrice = null;
            String originalDeepLink = null;


            Optional<Product> actualProduct = productRepo.findByName(productName);
            if (actualProduct.isPresent()) {
                originalPrice = actualProduct.get().getCurrentPrice();
                originalDeepLink = actualProduct.get().getDeepLink();
            }

            String cleanName = cleanProductName(productName);
            String prompt = String.format("find lowest current price %s online shopping Flipkart Amazon Myntra Nykaa Ajio Blinkit Mamaearth Shopsy with buy link ₹", cleanName);

            return null;

        } catch (Exception e) {
            log.error("Error performing single product search for '{}': {}", productName, e.getMessage());
            return null;
        }
    }

    private String cleanProductName(String productName) {
        return productName
                .replaceAll("\\b(with|and|for|the|in|on|at|of|by|from)\\b", "")
                .replaceAll("[^a-zA-Z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void sendPriceDropNotifications() {
        try {
            List<Product> productsWithDrops = getProductsWithPriceDrop();

            log.info("📢 Sending notifications for {} products with price drops", productsWithDrops.size());

            for (Product product : productsWithDrops) {
                if (product.getCurrentPrice() != null) {
                    notificationService.notifyPriceDropAsync(
                            product.getPid(),
                            product.getName(),
                            product.getCurrentPrice()
                    );
                }
            }
        } catch (Exception e) {
            log.error("Error sending price drop notifications: {}", e.getMessage());
        }
    }

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


                    if (i < productIds.size() - 1) {
                        Thread.sleep(2000);
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

            boolean priceDropped = false;
            if (oldPrice != null && newPrice < oldPrice) {
                priceDropped = true;
                product.setIsPriceDropped(true);


                if (product.getLastLowestPrice() == null || newPrice < product.getLastLowestPrice()) {
                    product.setLastLowestPrice(newPrice);
                }
            } else {
                product.setIsPriceDropped(false);
            }

            productRepo.save(product);
            log.info("Manually updated price for product {}: {} -> {}",
                    product.getName(), oldPrice, newPrice);


            if (priceDropped) {
                notificationService.notifyPriceDropAsync(productId, product.getName(), newPrice);
            }
        }, () -> {
            throw new RuntimeException("Product not found with ID: " + productId);
        });
    }

    @Scheduled(fixedRate = 14400000)
    public void scheduledPriceUpdate() {
        log.info("🕐 Starting scheduled comprehensive price update");
        try {
            updateAllProductPrices();
        } catch (Exception e) {
            log.error("Error during scheduled price update: {}", e.getMessage(), e);
        }
    }


    public Map<String, Object> getProductStatistics() {
        List<Product> allProducts = productRepo.findAll();

        long totalProducts = allProducts.size();
        long productsWithPrices = allProducts.stream()
                .filter(p -> p.getCurrentPrice() != null && p.getCurrentPrice() > 0)
                .count();
        long productsWithPriceDrops = allProducts.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsPriceDropped()))
                .count();
        long productsWithDeepLinks = allProducts.stream()
                .filter(p -> p.getDeepLink() != null && !p.getDeepLink().trim().isEmpty())
                .count();

        return Map.of(
                "totalProducts", totalProducts,
                "productsWithPrices", productsWithPrices,
                "productsWithPriceDrops", productsWithPriceDrops,
                "productsWithDeepLinks", productsWithDeepLinks,
                "percentageWithPrices", totalProducts > 0 ? (double) productsWithPrices / totalProducts * 100 : 0,
                "percentageWithDeepLinks", totalProducts > 0 ? (double) productsWithDeepLinks / totalProducts * 100 : 0
        );
    }

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

    public boolean productExistsByName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            return false;
        }
        return productRepo.findByName(productName.trim()).isPresent();
    }

    public Product findOrCreateProduct(String productName, String imageUrl) {
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty");
        }
        Optional<Product> existingProduct = productRepo.findByName(productName.trim());

        if (existingProduct.isPresent()) {
            log.debug("Found existing product: {}", productName);
            return existingProduct.get();
        }

        log.info("Creating new product with details: {}", productName);
        Product newProduct = new Product();
        newProduct.setName(productName.trim());
        newProduct.setImageUrl(imageUrl);
        parseAndSetProductDetails(newProduct);

        return productRepo.save(newProduct);
    }

    private void parseAndSetProductDetails(Product product) {
        try {
            product.setLastLowestPrice(null);
            product.setDeepLink(null);
            product.setIsPriceDropped(null);

            log.debug("Product details set for: {} with initial price: ₹{}",
                    product.getName(), product.getCurrentPrice());

        } catch (Exception e) {
            log.error("Error parsing product details for: {}", product.getName(), e);

            product.setCurrentPrice(0.0);
            product.setLastLowestPrice(null);
            product.setDeepLink(null);
            product.setIsPriceDropped(null);
        }
    }
}

package com.example.dealspy.service;

import com.example.dealspy.dto.WatchlistDTO;
import com.example.dealspy.dto.WatchlistResponseDTO;
import com.example.dealspy.mapper.WatchlistMapper;
import com.example.dealspy.model.Product;
import com.example.dealspy.model.User;
import com.example.dealspy.model.Watchlist;
import com.example.dealspy.repo.UserRepo;
import com.example.dealspy.repo.WatchListRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class WatchListService {

    private final WatchListRepo watchListRepo;
    private final UserRepo userRepo;
    private final WatchlistMapper mapper;
    private final ProductService productService;

    public WatchListService(WatchListRepo watchListRepo,
                            UserRepo userRepo,
                            WatchlistMapper mapper,
                            ProductService productService) {
        this.watchListRepo = watchListRepo;
        this.userRepo = userRepo;
        this.mapper = mapper;
        this.productService = productService;
    }

    /**
     * Get user's watchlist items
     * @param uid User ID
     * @return List of WatchlistResponseDTO
     */
    public List<WatchlistResponseDTO> getUserWatchList(String uid) {
        User user = userRepo.findById(uid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + uid));

        log.debug("Fetching watchlist items for user: {}", uid);

        return watchListRepo.findByUser(user)
                .stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    /**
     * Add product to watchlist
     * @param uid User ID
     * @param watchlistDTO WatchlistDTO containing product details and watch end date
     */
    @Transactional
    public void addToWatchList(String uid, WatchlistDTO watchlistDTO) {
        try {
            log.info("Adding to watchlist - User: {}, Product: {}, End Date: {}",
                    uid, watchlistDTO.getProductName(), watchlistDTO.getWatchEndDate());

            // Find user
            User user = userRepo.findById(uid)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + uid));

            // Find or create product using ProductService
            Product product = productService.findOrCreateProduct(
                    watchlistDTO.getProductName(),
                    watchlistDTO.getImageUrl(),
                    watchlistDTO.getDesc()
            );

            // Check if already exists
            boolean exists = watchListRepo.existsByUserAndProduct(user, product);
            if (exists) {
                log.info("Product already in watchlist - User: {}, Product: {}", uid, watchlistDTO.getProductName());
                throw new IllegalStateException("Product already in watchlist");
            }

            // Create watchlist entry
            Watchlist watchlist = new Watchlist();
            watchlist.setUser(user);
            watchlist.setProduct(product);
            watchlist.setWatchEndDate(watchlistDTO.getWatchEndDate());

            watchListRepo.save(watchlist);

            log.info("Successfully added to watchlist - User: {}, Product: {} (PID: {}), End Date: {}",
                    uid, watchlistDTO.getProductName(), product.getPid(), watchlistDTO.getWatchEndDate());

        } catch (UsernameNotFoundException | IllegalStateException e) {
            log.error("Error adding to watchlist: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to add to watchlist - User: {}, Product: {}",
                    uid, watchlistDTO.getProductName(), e);
            throw new RuntimeException("Failed to add to watchlist: " + e.getMessage());
        }
    }

    /**
     * Remove product from watchlist
     * @param uid User ID
     * @param productName Product name to remove
     */
    @Transactional
    public void deleteFromWatchList(String uid, String productName) {
        if (uid == null || productName == null) {
            throw new IllegalArgumentException("User ID and Product name cannot be null");
        }

        log.info("Removing from watchlist - User: {}, Product: {}", uid, productName);

        Product product = productService.getProductByName(productName)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productName));

        int deletedCount = watchListRepo.deleteByUserUidAndProductPid(uid, product.getPid());

        if (deletedCount == 0) {
            throw new EntityNotFoundException(
                    String.format("No watchlist entry found for user %s and product %s (PID: %d)",
                            uid, productName, product.getPid()));
        }

        log.info("Successfully removed from watchlist - User: {}, Product: {} (PID: {})",
                uid, productName, product.getPid());
    }

    /**
     * Remove expired watchlist items
     * @return Number of removed items
     */
    @Transactional
    public int removeExpiredWatchlistItems() {
        LocalDate today = LocalDate.now();

        log.info("Removing expired watchlist items (date: {})", today);

        List<Watchlist> expiredItems = watchListRepo.findByWatchEndDateBefore(today);
        int expiredCount = expiredItems.size();

        if (expiredCount > 0) {
            watchListRepo.deleteAll(expiredItems);
            log.info("Removed {} expired watchlist items", expiredCount);
        } else {
            log.debug("No expired watchlist items found");
        }

        return expiredCount;
    }

    /**
     * Get watchlist items expiring soon (within next 3 days)
     * @return List of watchlist items expiring soon
     */
    public List<WatchlistResponseDTO> getWatchlistExpiringSoon() {
        LocalDate today = LocalDate.now();
        LocalDate threeDaysFromNow = today.plusDays(3);

        log.debug("Fetching watchlist items expiring between {} and {}", today, threeDaysFromNow);

        return watchListRepo.findByWatchEndDateBetween(today, threeDaysFromNow)
                .stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    /**
     * Clear all watchlist items for a user
     * @param uid User ID
     */
    @Transactional
    public void clearAllWatchlist(String uid) {
        if (uid == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        log.info("Clearing all watchlist items for user: {}", uid);

        int deletedCount = watchListRepo.deleteByUserUid(uid);

        log.info("Cleared {} watchlist items for user: {}", deletedCount, uid);
    }

    /**
     * Get count of watchlist items for a user
     * @param uid User ID
     * @return Count of items
     */
    public long getWatchlistCount(String uid) {
        User user = userRepo.findById(uid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + uid));

        return watchListRepo.countByUser(user);
    }

    /**
     * Extend watchlist end date for a product
     * @param uid User ID
     * @param productName Product name
     * @param newEndDate New end date
     */
    @Transactional
    public void extendWatchlistEndDate(String uid, String productName, LocalDate newEndDate) {
        if (uid == null || productName == null || newEndDate == null) {
            throw new IllegalArgumentException("User ID, Product name, and new end date cannot be null");
        }

        log.info("Extending watchlist end date - User: {}, Product: {}, New Date: {}",
                uid, productName, newEndDate);

        User user = userRepo.findById(uid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + uid));

        Product product = productService.getProductByName(productName)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productName));

        Watchlist watchlist = watchListRepo.findByUserAndProduct(user, product)
                .orElseThrow(() -> new EntityNotFoundException("Watchlist entry not found"));

        LocalDate oldEndDate = watchlist.getWatchEndDate();
        watchlist.setWatchEndDate(newEndDate);

        watchListRepo.save(watchlist);

        log.info("Successfully extended watchlist end date - User: {}, Product: {}, {} -> {}",
                uid, productName, oldEndDate, newEndDate);
    }
}

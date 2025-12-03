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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class WatchListService {

    @Autowired
    private WatchListRepo watchListRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private WatchlistMapper mapper;

    @Autowired
    private ProductService productService;

    public List<WatchlistResponseDTO> getUserWatchList(String uid) {
        User user = userRepo.findById(uid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + uid));

        log.debug("Fetching watchlist items for user: {}", uid);

        return watchListRepo.findByUser(user)
                .stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    @Transactional
    public void addToWatchList(String uid, WatchlistDTO watchlistDTO) {
        if (uid == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (watchlistDTO == null || watchlistDTO.getProductName() == null) {
            throw new IllegalArgumentException("WatchlistDTO and product name cannot be null");
        }

        try {
            log.info("Adding to watchlist - User: {}, Product: {}",
                    uid, watchlistDTO.getProductName());


            User user = userRepo.findById(uid)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + uid));

            Product product = productService.findOrCreateProduct(
                    watchlistDTO.getProductName(),
                    watchlistDTO.getBrand(),
                    watchlistDTO.getPlatformName(),
                    watchlistDTO.getImageUrl(),
                    watchlistDTO.getDeepLink(),
                    watchlistDTO.getCurrentPrice(),
                    watchlistDTO.getCurrentPrice()
            );
            boolean exists = watchListRepo.existsByUserAndProduct(user, product);
            if (exists) {
                log.info("Product already in watchlist - User: {}, Product: {}", uid, watchlistDTO.getProductName());
                throw new IllegalStateException("Product already in watchlist");
            }

            Watchlist watchlist = new Watchlist();
            watchlist.setUser(user);
            watchlist.setProduct(product);

            watchListRepo.save(watchlist);

            log.info("Successfully added to watchlist - User: {}, Product: {} (PID: {})",
                    uid, watchlistDTO.getProductName(), product.getPid());

        } catch (UsernameNotFoundException | IllegalStateException e) {
            log.error("Error adding to watchlist: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to add to watchlist - User: {}, Product: {}",
                    uid, watchlistDTO.getProductName(), e);
            throw new RuntimeException("Failed to add to watchlist: " + e.getMessage());
        }
    }

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

    @Transactional
    public void clearAllWatchlist(String uid) {
        if (uid == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        log.info("Clearing all watchlist items for user: {}", uid);

        int deletedCount = watchListRepo.deleteByUserUid(uid);

        log.info("Cleared {} watchlist items for user: {}", deletedCount, uid);
    }


}

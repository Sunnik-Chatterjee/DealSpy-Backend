package com.example.dealspy.service;

import com.example.dealspy.dto.WatchlistDTO;
import com.example.dealspy.dto.WatchlistResponseDTO;
import com.example.dealspy.mapper.WatchlistMapper;
import com.example.dealspy.model.Product;
import com.example.dealspy.model.User;
import com.example.dealspy.model.Watchlist;
import com.example.dealspy.repo.ProductRepo;
import com.example.dealspy.repo.UserRepo;
import com.example.dealspy.repo.WatchListRepo;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WatchListService {
    private static final Logger logger = LoggerFactory.getLogger(WatchListService.class);

    private final WatchListRepo watchListRepo;
    private final UserRepo userRepo;
    private final ProductRepo productRepo;
    private final WatchlistMapper mapper;

    public WatchListService(WatchListRepo watchListRepo,
                            UserRepo userRepo,
                            ProductRepo productRepo,
                            WatchlistMapper mapper) {
        this.watchListRepo = watchListRepo;
        this.userRepo = userRepo;
        this.productRepo = productRepo;
        this.mapper = mapper;
    }

    // Get WatchList
    public List<WatchlistResponseDTO> getUserWatchList(String uid) {
        User user = userRepo.findById(uid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return watchListRepo.findByUser(user)
                .stream()
                .map(mapper::toResponseDTO)  // ✅ use mapper
                .toList();
    }

    // Post WatchList
    public void addToWatchList(String uid, WatchlistDTO watchList) {
        User user = userRepo.findById(uid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Product product = productRepo.findByName(watchList.getProductName())
                .orElseGet(() -> {
                    Product p = new Product();
                    p.setName(watchList.getProductName());
                    return productRepo.save(p);
                });

        boolean exists = watchListRepo.existsByUserAndProduct(user, product);
        if (exists) throw new IllegalStateException("Already in watchlist");

        Watchlist w = new Watchlist();
        w.setUser(user);
        w.setProduct(product);
        w.setWatchEndDate(watchList.getWatchEndDate());
        watchListRepo.save(w);
    }

    // Delete from watchlist
    @Transactional
    public void deleteFromWatchList(String uid, String productName) {
        if (uid == null || productName == null) {
            throw new IllegalArgumentException("User ID and Product name cannot be null");
        }

        Product product = productRepo.findByName(productName)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productName));

        int deletedCount = watchListRepo.deleteByUserUidAndProductPid(uid, product.getPid());

        if (deletedCount == 0) {
            throw new EntityNotFoundException(
                    String.format("No watchlist entry found for user %s and product %s (PID: %d)",
                            uid, productName, product.getPid()));
        }

        logger.info("Deleted watchlist entry - User: {}, Product: {} (PID: {})",
                uid, productName, product.getPid());
    }
}

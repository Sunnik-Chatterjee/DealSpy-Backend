package com.example.dealspy.service;

import com.example.dealspy.dto.SaveForLaterDTO;
import com.example.dealspy.mapper.SaveForLaterMapper;
import com.example.dealspy.model.Product;
import com.example.dealspy.model.SaveForLater;
import com.example.dealspy.model.User;
import com.example.dealspy.repo.SaveForLaterRepo;
import com.example.dealspy.repo.UserRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class SaveForLaterService {

    private final UserRepo userRepo;
    private final SaveForLaterRepo saveForLaterRepo;
    private final SaveForLaterMapper mapper;
    private final ProductService productService;

    public SaveForLaterService(UserRepo userRepo,
                               SaveForLaterRepo saveForLaterRepo,
                               SaveForLaterMapper mapper,
                               ProductService productService) {
        this.userRepo = userRepo;
        this.saveForLaterRepo = saveForLaterRepo;
        this.mapper = mapper;
        this.productService = productService;
    }

    /**
     * Get user's save for later items
     * @param uid User ID
     * @return List of SaveForLaterDTO
     */
    public List<SaveForLaterDTO> getUserSaveForLater(String uid) {
        User user = userRepo.findById(uid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + uid));

        log.debug("Fetching save for later items for user: {}", uid);

        return saveForLaterRepo.findByUser(user)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * Add product to save for later
     * @param uid User ID
     * @param dto SaveForLaterDTO containing product details
     * @return Success message
     */
    @Transactional
    public String addToSaveForLater(String uid, SaveForLaterDTO dto) {
        try {
            log.info("Adding to save for later - User: {}, Product: {}", uid, dto.getProductName());

            // Find user
            User user = userRepo.findById(uid)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + uid));

            // Find or create product using ProductService
            Product product = productService.findOrCreateProduct(
                    dto.getProductName(),
                    dto.getImageUrl(),
                    dto.getDesc()
            );

            // Check if already exists
            Optional<SaveForLater> existing = saveForLaterRepo.findByUserAndProduct(user, product);
            if (existing.isPresent()) {
                log.info("Product already in save for later - User: {}, Product: {}", uid, dto.getProductName());
                return "Product already in save for later";
            }

            // Create save for later entry
            SaveForLater saveForLater = new SaveForLater();
            saveForLater.setUser(user);
            saveForLater.setProduct(product);

            saveForLaterRepo.save(saveForLater);

            log.info("Successfully added to save for later - User: {}, Product: {} (PID: {})",
                    uid, dto.getProductName(), product.getPid());

            return "Product added to save for later successfully";

        } catch (UsernameNotFoundException e) {
            log.error("User not found: {}", uid);
            throw e;
        } catch (Exception e) {
            log.error("Failed to add to save for later - User: {}, Product: {}",
                    uid, dto.getProductName(), e);
            throw new RuntimeException("Failed to add to save for later: " + e.getMessage());
        }
    }

    /**
     * Remove product from save for later
     * @param uid User ID
     * @param productName Product name to remove
     */
    @Transactional
    public void deleteFromSaveForLater(String uid, String productName) {
        if (uid == null || productName == null) {
            throw new IllegalArgumentException("User ID and Product name cannot be null");
        }

        log.info("Removing from save for later - User: {}, Product: {}", uid, productName);

        Product product = productService.getProductByName(productName)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productName));

        int deletedCount = saveForLaterRepo.deleteByUserUidAndProductPid(uid, product.getPid());

        if (deletedCount == 0) {
            throw new EntityNotFoundException(
                    String.format("No save-for-later entry found for user %s and product %s (PID: %d)",
                            uid, productName, product.getPid()));
        }

        log.info("Successfully removed from save for later - User: {}, Product: {} (PID: {})",
                uid, productName, product.getPid());
    }

    /**
     * Clear all save for later items for a user
     * @param uid User ID
     */
    @Transactional
    public void clearAllSaveForLater(String uid) {
        if (uid == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        log.info("Clearing all save for later items for user: {}", uid);

        int deletedCount = saveForLaterRepo.deleteByUserUid(uid);

        log.info("Cleared {} save for later items for user: {}", deletedCount, uid);
    }

    /**
     * Get count of save for later items for a user
     * @param uid User ID
     * @return Count of items
     */
    public long getSaveForLaterCount(String uid) {
        User user = userRepo.findById(uid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + uid));

        return saveForLaterRepo.countByUser(user);
    }
}

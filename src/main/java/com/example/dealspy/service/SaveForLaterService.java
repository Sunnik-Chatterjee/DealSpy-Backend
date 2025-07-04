package com.example.dealspy.service;

import com.example.dealspy.dto.SaveForLaterDTO;
import com.example.dealspy.model.Product;
import com.example.dealspy.model.SaveForLater;
import com.example.dealspy.model.User;
import com.example.dealspy.repo.ProductRepo;
import com.example.dealspy.repo.SaveForLaterRepo;
import com.example.dealspy.repo.UserRepo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SaveForLaterService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private SaveForLaterRepo saveForLaterRepo;

    public List<SaveForLaterDTO> getUserSaveForLater(String uid) {
        User user = userRepo.findById(uid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return saveForLaterRepo.findByUser(user)
                .stream()
                .map(save -> new SaveForLaterDTO(save.getProduct().getName()))
                .toList();
    }

    public String addToSaveForLater(String uid, SaveForLaterDTO dto) {
        User user = userRepo.findById(uid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Product product = productRepo.findByName(dto.getProductName())
                .orElseGet(() -> {
                    Product p = new Product();
                    p.setName(dto.getProductName());
                    return productRepo.save(p);
                });

        boolean exists = saveForLaterRepo.existsByUserAndProduct(user, product);
        if (exists) {
            return "Product already exists in Save For Later";
        }

        SaveForLater saveForLater = new SaveForLater();
        saveForLater.setUser(user);
        saveForLater.setProduct(product);

        saveForLaterRepo.save(saveForLater);

        return "Product added to Save For Later successfully";
    }

    @Transactional
    public void deleteFromSaveForLater(String uid, String productName) {
        // Input validation
        if (uid == null || productName == null) {
            throw new IllegalArgumentException("User ID and Product name cannot be null");
        }

        // Find product first (more efficient than loading user first)
        Product product = productRepo.findByName(productName)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Product not found: " + productName));

        // Execute deletion and verify
        int deletedCount = saveForLaterRepo.deleteByUserUidAndProductPid(uid, product.getPid());

        if (deletedCount == 0) {
            throw new EntityNotFoundException(
                    String.format("No watchlist entry found for user %s and product %s (PID: %d)",
                            uid, productName, product.getPid()));
        }
    }
}

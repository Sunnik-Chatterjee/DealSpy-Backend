package com.example.dealspy.service;

import com.example.dealspy.dto.SaveForLaterDTO;
import com.example.dealspy.dto.SaveForLaterResponseDTO;
import com.example.dealspy.mapper.SaveForLaterMapper;
import com.example.dealspy.model.Product;
import com.example.dealspy.model.SaveForLater;
import com.example.dealspy.model.User;
import com.example.dealspy.repo.ProductRepo;
import com.example.dealspy.repo.SaveForLaterRepo;
import com.example.dealspy.repo.UserRepo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SaveForLaterService {

    private final UserRepo userRepo;
    private final ProductRepo productRepo;
    private final SaveForLaterRepo saveForLaterRepo;
    private final SaveForLaterMapper mapper;

    public SaveForLaterService(UserRepo userRepo,
                               ProductRepo productRepo,
                               SaveForLaterRepo saveForLaterRepo,
                               SaveForLaterMapper mapper) {
        this.userRepo = userRepo;
        this.productRepo = productRepo;
        this.saveForLaterRepo = saveForLaterRepo;
        this.mapper = mapper;
    }

    public List<SaveForLaterDTO> getUserSaveForLater(String uid) {
        User user = userRepo.findById(uid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return saveForLaterRepo.findByUser(user)
                .stream()
                .map(mapper::toDTO)  // ✅ use mapper
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
        if (uid == null || productName == null) {
            throw new IllegalArgumentException("User ID and Product name cannot be null");
        }

        Product product = productRepo.findByName(productName)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productName));

        int deletedCount = saveForLaterRepo.deleteByUserUidAndProductPid(uid, product.getPid());

        if (deletedCount == 0) {
            throw new EntityNotFoundException(
                    String.format("No save-for-later entry found for user %s and product %s (PID: %d)",
                            uid, productName, product.getPid()));
        }
    }
}

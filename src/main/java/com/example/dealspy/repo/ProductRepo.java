package com.example.dealspy.repo;

import com.example.dealspy.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepo extends JpaRepository<Product, Integer> {  // ✅ Add generic types
    Optional<Product> findByName(String productName);
    boolean existsByName(String name);
}

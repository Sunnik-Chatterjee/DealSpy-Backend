package com.example.dealspy.repo;

import com.example.dealspy.dto.SaveForLaterDTO;
import com.example.dealspy.model.Product;
import com.example.dealspy.model.SaveForLater;
import com.example.dealspy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Arrays;
import java.util.List;

public interface SaveForLaterRepo extends JpaRepository<SaveForLater,Long> {
    List<SaveForLater> findByUser(User user);

    boolean existsByUserAndProduct(User user, Product product);

    <T> ScopedValue<T> findByUserAndProduct(User user, Product product);
}

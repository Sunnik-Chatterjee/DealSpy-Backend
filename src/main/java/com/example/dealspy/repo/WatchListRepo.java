package com.example.dealspy.repo;
import com.example.dealspy.model.Product;
import com.example.dealspy.model.User;
import com.example.dealspy.model.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchListRepo extends JpaRepository<Watchlist,Long> {
    List<Watchlist> findByUser(User user);

    boolean existsByUserAndProduct(User user, Product product);

    <T> ScopedValue<T> findByUserAndProduct(User user, Product product);
}

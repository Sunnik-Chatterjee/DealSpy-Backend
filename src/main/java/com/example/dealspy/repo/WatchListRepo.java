package com.example.dealspy.repo;
import com.example.dealspy.model.Product;
import com.example.dealspy.model.User;
import com.example.dealspy.model.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WatchListRepo extends JpaRepository<Watchlist,Long> {
    List<Watchlist> findByUser(User user);

    boolean existsByUserAndProduct(User user, Product product);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Watchlist w WHERE w.user.uid = :uid AND w.product.pid = :pid")
    int deleteByUserUidAndProductPid(@Param("uid") String uid,
                                     @Param("pid") Integer pid);  // Matches Product.pid type
}

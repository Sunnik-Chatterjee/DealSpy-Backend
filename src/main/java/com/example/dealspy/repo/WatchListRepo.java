package com.example.dealspy.repo;

import com.example.dealspy.model.Product;
import com.example.dealspy.model.User;
import com.example.dealspy.model.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WatchListRepo extends JpaRepository<Watchlist, Long> {

    List<Watchlist> findByUser(User user);

    @Query("SELECT w.user FROM Watchlist w WHERE w.product.pid = :pid")
    List<User> findUsersByProductId(@Param("pid") Integer pid);

    boolean existsByUserAndProduct(User user, Product product);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Watchlist w WHERE w.user.uid = :uid AND w.product.pid = :pid")
    int deleteByUserUidAndProductPid(@Param("uid") String uid, @Param("pid") Integer pid);

    long countByUser(User user);
    Optional<Watchlist> findByUserAndProduct(User user, Product product);
    List<Watchlist> findByWatchEndDateBefore(LocalDate date);

    List<Watchlist> findByWatchEndDateBetween(LocalDate startDate, LocalDate endDate);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Watchlist w WHERE w.user.uid = :uid")
    int deleteByUserUid(@Param("uid") String uid);

}

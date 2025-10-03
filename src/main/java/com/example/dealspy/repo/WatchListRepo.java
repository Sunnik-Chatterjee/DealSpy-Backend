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

    // ✅ Existing methods - all good
    List<Watchlist> findByUser(User user);

    @Query("SELECT w.user FROM Watchlist w WHERE w.product.pid = :pid")
    List<User> findUsersByProductId(@Param("pid") Integer pid);

    boolean existsByUserAndProduct(User user, Product product);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Watchlist w WHERE w.user.uid = :uid AND w.product.pid = :pid")
    int deleteByUserUidAndProductPid(@Param("uid") String uid, @Param("pid") Integer pid);

    long countByUser(User user);

    // ✅ NEW: Additional methods needed for complete WatchListService functionality

    /**
     * Find watchlist entry by user and product (needed for extending dates)
     */
    Optional<Watchlist> findByUserAndProduct(User user, Product product);

    /**
     * Find expired watchlist items (for cleanup)
     */
    List<Watchlist> findByWatchEndDateBefore(LocalDate date);

    /**
     * Find watchlist items expiring within a date range (for notifications)
     */
    List<Watchlist> findByWatchEndDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Delete all watchlist items for a user (for clear all functionality)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Watchlist w WHERE w.user.uid = :uid")
    int deleteByUserUid(@Param("uid") String uid);

    // ✅ OPTIONAL: Additional useful queries

    /**
     * Find watchlist items by product ID (useful for notifications)
     */
    @Query("SELECT w FROM Watchlist w WHERE w.product.pid = :pid")
    List<Watchlist> findByProductPid(@Param("pid") Integer pid);

    /**
     * Find active watchlist items (not expired)
     */
    @Query("SELECT w FROM Watchlist w WHERE w.watchEndDate >= :currentDate")
    List<Watchlist> findActiveWatchlistItems(@Param("currentDate") LocalDate currentDate);

    /**
     * Count expired watchlist items
     */
    long countByWatchEndDateBefore(LocalDate date);

    /**
     * Find user's watchlist items that are active
     */
    @Query("SELECT w FROM Watchlist w WHERE w.user = :user AND w.watchEndDate >= :currentDate")
    List<Watchlist> findActiveWatchlistByUser(@Param("user") User user, @Param("currentDate") LocalDate currentDate);
}

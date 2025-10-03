package com.example.dealspy.repo;

import com.example.dealspy.model.Product;
import com.example.dealspy.model.SaveForLater;
import com.example.dealspy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaveForLaterRepo extends JpaRepository<SaveForLater, Long> {

    // ✅ Existing methods - all perfect for basic functionality
    List<SaveForLater> findByUser(User user);

    boolean existsByUserAndProduct(User user, Product product);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM SaveForLater w WHERE w.user.uid = :uid AND w.product.pid = :pid")
    int deleteByUserUidAndProductPid(@Param("uid") String uid, @Param("pid") Integer pid);

    Optional<SaveForLater> findByUserAndProduct(User user, Product product);

    long countByUser(User user);

    // ✅ NEW: Only one missing method needed for complete functionality
    /**
     * Delete all save for later items for a user (for clear all functionality)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM SaveForLater sfl WHERE sfl.user.uid = :uid")
    int deleteByUserUid(@Param("uid") String uid);

    // ✅ OPTIONAL: Additional useful queries (not required but nice to have)

    /**
     * Find save for later items by product ID (useful for notifications when price drops)
     */
    @Query("SELECT sfl FROM SaveForLater sfl WHERE sfl.product.pid = :pid")
    List<SaveForLater> findByProductPid(@Param("pid") Integer pid);

    /**
     * Get users who have saved a specific product (for price drop notifications)
     */
    @Query("SELECT sfl.user FROM SaveForLater sfl WHERE sfl.product.pid = :pid")
    List<User> findUsersByProductId(@Param("pid") Integer pid);

    /**
     * Count total save for later items across all users (for statistics)
     */
    @Query("SELECT COUNT(sfl) FROM SaveForLater sfl")
    long countAllSaveForLaterItems();
}

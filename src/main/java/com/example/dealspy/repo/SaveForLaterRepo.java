package com.example.dealspy.repo;

import com.example.dealspy.dto.SaveForLaterDTO;
import com.example.dealspy.model.Product;
import com.example.dealspy.model.SaveForLater;
import com.example.dealspy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Arrays;
import java.util.List;

public interface SaveForLaterRepo extends JpaRepository<SaveForLater,Long> {
    List<SaveForLater> findByUser(User user);

    boolean existsByUserAndProduct(User user, Product product);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM SaveForLater w WHERE w.user.uid = :uid AND w.product.pid = :pid")
    int deleteByUserUidAndProductPid(@Param("uid") String uid,
                                     @Param("pid") Integer pid);  // Matches Product.pid type

}

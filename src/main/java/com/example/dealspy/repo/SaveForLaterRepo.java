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

    List<SaveForLater> findByUser(User user);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM SaveForLater w WHERE w.user.uid = :uid AND w.product.pid = :pid")
    int deleteByUserUidAndProductPid(@Param("uid") String uid, @Param("pid") Integer pid);

    Optional<SaveForLater> findByUserAndProduct(User user, Product product);

}

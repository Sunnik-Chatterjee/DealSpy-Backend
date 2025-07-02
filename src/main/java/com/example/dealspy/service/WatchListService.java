package com.example.dealspy.service;

import com.example.dealspy.dto.WatchlistDTO;
import com.example.dealspy.mapper.WatchlistMapper;
import com.example.dealspy.model.Product;
import com.example.dealspy.model.User;
import com.example.dealspy.model.Watchlist;
import com.example.dealspy.repo.ProductRepo;
import com.example.dealspy.repo.UserRepo;
import com.example.dealspy.repo.WatchListRepo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WatchListService {

    @Autowired
    private WatchListRepo watchListRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ProductRepo productRepo;

    //Get WatchList
    public List<WatchlistDTO> getUserWatchList(String uid){
        User user = userRepo.findById(uid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return watchListRepo.findByUser(user).stream()
                .map(WatchlistMapper::toDTO)
                .toList();
    }

    //Post WatchList
    public void addToWatchList(String uid,WatchlistDTO watchList){
        User user = userRepo.findById(uid)
                .orElseThrow(()-> new UsernameNotFoundException("User not found"));
        Product product = productRepo.findByName(watchList.getProductName())
                .orElseGet(() -> {
                    Product p = new Product();
                    p.setName(watchList.getProductName());
                    return productRepo.save(p);
                });
        boolean exists = watchListRepo.existsByUserAndProduct(user, product);
        if (exists) throw new IllegalStateException("Already in watchlist");

        Watchlist w = new Watchlist();
        w.setUser(user);
        w.setProduct(product);
        w.setWatchEndDate(watchList.getWatchEndDate());
        watchListRepo.save(w);
    }

    //Delete from watchlist

    public void deleteFromWatchList(String uid, String productName){
        User user = userRepo.findById(uid)
                .orElseThrow(()-> new UsernameNotFoundException("User not found"));
        Product product = productRepo.findByName(productName)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        Watchlist entry = (Watchlist) watchListRepo.findByUserAndProduct(user, product)
                .orElseThrow(() -> new EntityNotFoundException("Watchlist entry not found"));
        watchListRepo.delete(entry);
    }
}

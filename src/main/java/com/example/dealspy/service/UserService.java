package com.example.dealspy.service;

import com.example.dealspy.dto.SaveForLaterDTO;
import com.example.dealspy.dto.UserDetailDTO;
import com.example.dealspy.dto.WatchlistDTO;
import com.example.dealspy.model.User;
import com.example.dealspy.repo.SaveForLaterRepo;
import com.example.dealspy.repo.UserRepo;
import com.example.dealspy.repo.WatchListRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private WatchListRepo watchListRepo;

    @Autowired
    private SaveForLaterRepo saveForLaterRepo;

    public UserDetailDTO getUser(String uid){
        User user = userRepo.findById(uid).
                orElseThrow(()->new UsernameNotFoundException("User not found"));

        List<WatchlistDTO> watchlistDTOs = watchListRepo.findByUser(user).stream().map(w->new WatchlistDTO(
                w.getProduct().getName(), w.getWatchEndDate()
        )).toList();

        List<SaveForLaterDTO> saveForLaterDTOS = saveForLaterRepo.findByUser(user).stream().map(s->new SaveForLaterDTO(
                s.getProduct().getName()
        )).toList();

        return new UserDetailDTO(watchlistDTOs,saveForLaterDTOS);
    }
}

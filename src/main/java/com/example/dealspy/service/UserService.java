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
    private FCMService fcmService;
    @Autowired
    private UserRepo userRepo;

    @Autowired
    private WatchListRepo watchListRepo;

    @Autowired
    private SaveForLaterRepo saveForLaterRepo;

    public UserDetailDTO getUser(String uid) {
        User user = userRepo.findById(uid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<WatchlistDTO> watchlistDTOs = watchListRepo.findByUser(user).stream()
                .map(w -> new WatchlistDTO(
                        w.getProduct().getName(),
                        w.getWatchEndDate()
                )).toList();

        List<SaveForLaterDTO> saveForLaterDTOS = saveForLaterRepo.findByUser(user).stream()
                .map(s -> new SaveForLaterDTO(
                        s.getProduct().getName()
                )).toList();

        return new UserDetailDTO(watchlistDTOs, saveForLaterDTOS);
    }

    public Boolean isUserExist(String uid) {
        return userRepo.existsById(uid);
    }

    public void addUserDetails(String uid, String email, String name, String fcmToken) {
        User user = new User();
        user.setUid(uid);
        user.setName(name);
        user.setEmail(email);
        user.setFcmToken(fcmToken);  // Add FCM token
        userRepo.save(user);
    }

    public void updateUserFcmToken(String uid, String fcmToken) {
        if (fcmToken == null) return;

        User user = userRepo.findById(uid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFcmToken(fcmToken);
        userRepo.save(user);
    }

    public void sendNotificationToUser(String uid, String title, String body) {
        User user = userRepo.findById(uid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String fcmToken = user.getFcmToken();

        if (fcmToken == null) {
            throw new RuntimeException("FCM token not available for user: " + uid);
        }

        fcmService.sendNotificationToToken(fcmToken, title, body);
    }

}

package com.example.dealspy.service;

import com.example.dealspy.dto.SaveForLaterDTO;
import com.example.dealspy.dto.UserDetailDTO;
import com.example.dealspy.dto.WatchlistDTO;
import com.example.dealspy.mapper.SaveForLaterMapper;
import com.example.dealspy.mapper.WatchlistMapper;
import com.example.dealspy.model.User;
import com.example.dealspy.repo.SaveForLaterRepo;
import com.example.dealspy.repo.UserRepo;
import com.example.dealspy.repo.WatchListRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepo userRepo;
    @Autowired
    private WatchListRepo watchListRepo;
    @Autowired
    private SaveForLaterRepo saveForLaterRepo;
    @Autowired
    private WatchlistMapper watchlistMapper;
    @Autowired
    private SaveForLaterMapper saveForLaterMapper;



    public UserDetailDTO getUser(String uid) {
        User user = userRepo.findById(uid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with uid: " + uid));


        List<WatchlistDTO> watchlistDTOs = watchlistMapper.toDTOList(
                watchListRepo.findByUser(user)
        );

        List<SaveForLaterDTO> saveForLaterDTOs = saveForLaterMapper.toDTOList(
                saveForLaterRepo.findByUser(user)
        );

        log.info("Retrieved user profile for uid: {} with {} watchlist items and {} save-for-later items",
                uid, watchlistDTOs.size(), saveForLaterDTOs.size());

        return new UserDetailDTO(watchlistDTOs, saveForLaterDTOs);
    }

    public Boolean isUserExist(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return false;
        }
        return userRepo.existsById(uid);
    }

    @Transactional
    public User addUserDetails(String uid, String email, String name, String fcmToken) {
        if (uid == null || uid.trim().isEmpty()) {
            throw new IllegalArgumentException("User UID cannot be null or empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("User email cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be null or empty");
        }
        User user = userRepo.findById(uid).orElse(new User());

        user.setUid(uid);
        user.setName(name.trim());
        user.setEmail(email.trim().toLowerCase());
        if (fcmToken != null && !fcmToken.trim().isEmpty()) {
            user.setFcmToken(fcmToken.trim());
        }

        User savedUser = userRepo.save(user);
        log.info("User details saved/updated for uid: {} with email: {}", uid, email);
        return savedUser;
    }

    @Transactional
    public boolean updateUserFcmToken(String uid, String fcmToken) {
        if (uid == null || uid.trim().isEmpty()) {
            log.warn("Cannot update FCM token: UID is null or empty");
            return false;
        }

        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            log.warn("Cannot update FCM token: token is null or empty for user {}", uid);
            return false;
        }

        Optional<User> userOpt = userRepo.findById(uid);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setFcmToken(fcmToken.trim());
            userRepo.save(user);
            log.info("Updated FCM token for user: {}", uid);
            return true;
        } else {
            log.warn("User not found for FCM token update: {}", uid);
            return false;
        }
    }


    @Transactional
    public boolean deleteUser(String uid) {
        try {
            Optional<User> userOptional = userRepo.findByUid(uid);

            if (userOptional.isPresent()) {
                User user = userOptional.get();

                log.info("Deleting user profile: UID={}, Email={}", uid, user.getEmail());

                userRepo.delete(user);
                log.info("User profile deleted successfully: UID={}", uid);

                return true;
            } else {
                log.warn("User not found for deletion: UID={}", uid);
                return false;
            }
        } catch (Exception e) {
            log.error("Error deleting user with UID {}: {}", uid, e.getMessage());
            throw new RuntimeException("Failed to delete user profile", e);
        }
    }

}

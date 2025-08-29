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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserService {

    // ✅ All dependencies via constructor injection
    private final FCMService fcmService;
    private final UserRepo userRepo;
    private final WatchListRepo watchListRepo;
    private final SaveForLaterRepo saveForLaterRepo;
    private final WatchlistMapper watchlistMapper;
    private final SaveForLaterMapper saveForLaterMapper;

    public UserService(FCMService fcmService,
                       UserRepo userRepo,
                       WatchListRepo watchListRepo,
                       SaveForLaterRepo saveForLaterRepo,
                       WatchlistMapper watchlistMapper,
                       SaveForLaterMapper saveForLaterMapper) {
        this.fcmService = fcmService;
        this.userRepo = userRepo;
        this.watchListRepo = watchListRepo;
        this.saveForLaterRepo = saveForLaterRepo;
        this.watchlistMapper = watchlistMapper;
        this.saveForLaterMapper = saveForLaterMapper;
    }

    // ✅ Get user profile with proper DTO mapping
    public UserDetailDTO getUser(String uid) {
        User user = userRepo.findById(uid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with uid: " + uid));

        // Use mappers to convert entities to DTOs
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

    // ✅ Check if user exists
    public Boolean isUserExist(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return false;
        }
        return userRepo.existsById(uid);
    }

    // ✅ Find user by ID
    public Optional<User> findUserById(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return Optional.empty();
        }
        return userRepo.findById(uid);
    }

    // ✅ Add/Update user details (upsert logic)
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

        // Find existing user or create new one
        User user = userRepo.findById(uid).orElse(new User());

        // Set user details
        user.setUid(uid);
        user.setName(name.trim());
        user.setEmail(email.trim().toLowerCase());

        // Set FCM token if provided
        if (fcmToken != null && !fcmToken.trim().isEmpty()) {
            user.setFcmToken(fcmToken.trim());
        }

        User savedUser = userRepo.save(user);
        log.info("User details saved/updated for uid: {} with email: {}", uid, email);
        return savedUser;
    }

    // ✅ Update user FCM token
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

    // ✅ Update user profile information
    @Transactional
    public boolean updateUserProfile(String uid, String name, String email) {
        if (uid == null || uid.trim().isEmpty()) {
            throw new IllegalArgumentException("User UID cannot be null or empty");
        }

        Optional<User> userOpt = userRepo.findById(uid);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (name != null && !name.trim().isEmpty()) {
                user.setName(name.trim());
            }

            if (email != null && !email.trim().isEmpty()) {
                user.setEmail(email.trim().toLowerCase());
            }

            userRepo.save(user);
            log.info("Updated profile for user: {}", uid);
            return true;
        } else {
            log.warn("User not found for profile update: {}", uid);
            return false;
        }
    }

    // ✅ Clear invalid FCM token
    @Transactional
    public void clearFcmTokenByToken(String fcmToken) {
        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            return;
        }

        List<User> users = userRepo.findAll().stream()
                .filter(user -> fcmToken.equals(user.getFcmToken()))
                .toList();

        for (User user : users) {
            user.setFcmToken(null);
            userRepo.save(user);
            log.info("Cleared invalid FCM token for user: {}", user.getUid());
        }
    }

    // ✅ Clear FCM token for specific user
    @Transactional
    public boolean clearUserFcmToken(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return false;
        }

        Optional<User> userOpt = userRepo.findById(uid);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setFcmToken(null);
            userRepo.save(user);
            log.info("Cleared FCM token for user: {}", uid);
            return true;
        }
        return false;
    }

    // ✅ Send notification to specific user
    public boolean sendNotificationToUser(String uid, String title, String body) {
        if (uid == null || uid.trim().isEmpty()) {
            log.warn("Cannot send notification: UID is null or empty");
            return false;
        }

        if (title == null || title.trim().isEmpty() || body == null || body.trim().isEmpty()) {
            log.warn("Cannot send notification: title or body is empty for user {}", uid);
            return false;
        }

        Optional<User> userOpt = userRepo.findById(uid);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String fcmToken = user.getFcmToken();

            if (fcmToken == null || fcmToken.trim().isEmpty()) {
                log.warn("FCM token not available for user: {}", uid);
                return false;
            }

            // Send async notification
            fcmService.sendNotificationToTokenAsync(fcmToken, title.trim(), body.trim());
            log.info("Notification sent to user: {}", uid);
            return true;
        } else {
            log.warn("User not found for notification: {}", uid);
            return false;
        }
    }

    // ✅ Delete user and all related data
    @Transactional
    public boolean deleteUser(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return false;
        }

        Optional<User> userOpt = userRepo.findById(uid);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Delete related watchlist entries
            watchListRepo.deleteAll(watchListRepo.findByUser(user));

            // Delete related save-for-later entries
            saveForLaterRepo.deleteAll(saveForLaterRepo.findByUser(user));

            // Delete user
            userRepo.delete(user);

            log.info("Deleted user and all related data for uid: {}", uid);
            return true;
        }
        return false;
    }

    // ✅ Get total user count
    public long getUserCount() {
        return userRepo.count();
    }

    // ✅ Get all users (for admin purposes)
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }
}

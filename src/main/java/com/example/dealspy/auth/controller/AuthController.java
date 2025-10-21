package com.example.dealspy.auth.controller;

import com.example.dealspy.common.ApiResponse;
import com.example.dealspy.service.UserService;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyUser(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader(value = "X-FCM-TOKEN", required = false) String fcmToken) {

        try {
            logger.info("=== VERIFY ENDPOINT CALLED ===");

            // CRITICAL: Check if Firebase is initialized
            if (FirebaseApp.getApps().isEmpty()) {
                logger.error("❌ Firebase is NOT initialized! Cannot verify users.");
                return ResponseEntity.status(500).body(
                        new ApiResponse<>(false, "Server configuration error - Firebase not initialized", null)
                );
            }

            logger.info("✅ Firebase is initialized. Apps count: {}", FirebaseApp.getApps().size());

            // Validate Authorization header
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("❌ Invalid Authorization header received");
                return ResponseEntity.badRequest().body(
                        new ApiResponse<>(false, "Missing or invalid Authorization header", null)
                );
            }

            // Extract token
            String token = authHeader.substring(7);
            logger.info("Token extracted, length: {}", token.length());

            // Verify Firebase token
            FirebaseToken decodedToken;
            try {
                decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
                logger.info("✅ Token verified successfully");
            } catch (FirebaseAuthException e) {
                logger.warn("❌ Firebase authentication failed: {} - {}", e.getAuthErrorCode(), e.getMessage());
                return ResponseEntity.status(401).body(
                        new ApiResponse<>(false, "Invalid or expired Firebase token", null)
                );
            }

            // Extract user info
            String uid = decodedToken.getUid();
            String name = decodedToken.getName();
            String email = decodedToken.getEmail();

            logger.info("User info - UID: {}, Email: {}, Name: {}", uid, email, name);

            // Save or update user in database
            try {
                if (!userService.isUserExist(uid)) {
                    userService.addUserDetails(uid, email, name, fcmToken);
                    logger.info("✅ New user saved to DB: {}", uid);
                } else {
                    logger.info("User already exists in DB: {}", uid);
                    if (fcmToken != null && !fcmToken.isEmpty()) {
                        userService.updateUserFcmToken(uid, fcmToken);
                        logger.info("✅ FCM token updated for user: {}", uid);
                    }
                }
            } catch (Exception dbException) {
                logger.error("❌ Database operation failed", dbException);
                return ResponseEntity.status(500).body(
                        new ApiResponse<>(false, "Database error: " + dbException.getMessage(), null)
                );
            }

            logger.info("=== VERIFY COMPLETED SUCCESSFULLY ===");
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "User authenticated successfully", null)
            );

        } catch (IllegalStateException e) {
            logger.error("❌ Firebase IllegalStateException - Firebase not properly initialized", e);
            return ResponseEntity.status(500).body(
                    new ApiResponse<>(false, "Server configuration error - Firebase initialization failed", null)
            );
        } catch (Exception e) {
            logger.error("❌ Unexpected error during authentication", e);
            logger.error("Error type: {}", e.getClass().getName());
            logger.error("Error message: {}", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                    new ApiResponse<>(false, "Authentication failed: " + e.getMessage(), null)
            );
        }
    }
}

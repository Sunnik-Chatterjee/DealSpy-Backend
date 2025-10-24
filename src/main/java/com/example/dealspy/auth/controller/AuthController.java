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

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Check Firebase initialization
            if (FirebaseApp.getApps().isEmpty()) {
                health.put("firebase", "NOT_INITIALIZED");
                health.put("status", "ERROR");
                return ResponseEntity.status(500).body(health);
            }

            // Try to access Firebase Auth
            FirebaseAuth.getInstance();
            health.put("firebase", "CONNECTED");

            // Try a simple operation
            FirebaseAuth.getInstance().listUsers(null, 1);
            health.put("firebase_auth", "WORKING");
            health.put("status", "OK");

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            health.put("firebase", "ERROR");
            health.put("error", e.getMessage());
            health.put("status", "ERROR");
            return ResponseEntity.status(500).body(health);
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyUser(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader(value = "X-FCM-TOKEN", required = false) String fcmToken) {

        try {
            // Check Firebase initialization
            if (FirebaseApp.getApps().isEmpty()) {
                logger.error("Firebase not initialized");
                return errorResponse(500, "Server configuration error - Firebase not initialized");
            }

            // Validate and extract token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("Invalid Authorization header");
                return errorResponse(400, "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);

            // Verify Firebase token
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);

            String uid = decodedToken.getUid();
            String name = decodedToken.getName();
            String email = decodedToken.getEmail();

            logger.info("User verified - UID: {}, Email: {}", uid, email);

            // Handle user database operations
            handleUserDatabase(uid, email, name, fcmToken);

            return ResponseEntity.ok(
                    new ApiResponse<>(true, "User authenticated successfully", null)
            );

        } catch (FirebaseAuthException e) {
            logger.warn("Firebase auth failed: {}", e.getMessage());
            return errorResponse(401, "Invalid or expired Firebase token");
        } catch (IllegalStateException e) {
            logger.error("Firebase initialization error", e);
            return errorResponse(500, "Server configuration error - Firebase initialization failed");
        } catch (Exception e) {
            logger.error("Unexpected error during authentication", e);
            return errorResponse(500, "Authentication failed: " + e.getMessage());
        }
    }

    private void handleUserDatabase(String uid, String email, String name, String fcmToken) {
        try {
            if (!userService.isUserExist(uid)) {
                userService.addUserDetails(uid, email, name, fcmToken);
                logger.info("New user saved: {}", uid);
            } else if (fcmToken != null && !fcmToken.isEmpty()) {
                userService.updateUserFcmToken(uid, fcmToken);
                logger.info("FCM token updated for: {}", uid);
            }
        } catch (Exception e) {
            logger.error("Database operation failed", e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    private ResponseEntity<ApiResponse<String>> errorResponse(int status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(false, message, null));
    }
}

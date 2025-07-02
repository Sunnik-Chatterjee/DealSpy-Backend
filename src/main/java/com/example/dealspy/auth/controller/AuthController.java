package com.example.dealspy.auth.controller;

import com.example.dealspy.common.ApiResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyUser(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("Invalid Authorization header received");
                return ResponseEntity.badRequest().body(
                        new ApiResponse<>(false, "Missing or invalid Authorization header", null)
                );
            }

            String token = authHeader.substring(7);
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);

            logger.info("User verified successfully: {}", decodedToken.getUid());

            return ResponseEntity.ok(
                    new ApiResponse<>(true, "User authenticated successfully", null)
            );

        } catch (FirebaseAuthException e) {
            logger.warn("Firebase authentication failed: {}", e.getAuthErrorCode());
            return ResponseEntity.status(401).body(
                    new ApiResponse<>(false, "Invalid or expired Firebase token", null)
            );
        } catch (Exception e) {
            logger.error("Unexpected error during authentication", e);
            return ResponseEntity.status(500).body(
                    new ApiResponse<>(false, "Authentication failed", null)
            );
        }
    }
}
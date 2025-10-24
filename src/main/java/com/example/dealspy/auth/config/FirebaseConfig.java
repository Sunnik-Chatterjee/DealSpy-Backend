package com.example.dealspy.auth.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.config.path}")
    private String firebaseConfigPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        logger.info("=== FIREBASE INITIALIZATION START ===");
        logger.info("Config path: {}", firebaseConfigPath);

        if (FirebaseApp.getApps().isEmpty()) {
            try {
                InputStream serviceAccount;
                if (firebaseConfigPath.startsWith("/")) {
                    File configFile = new File(firebaseConfigPath);
                    if (!configFile.exists()) {
                        throw new RuntimeException("Firebase service account file not found: " + firebaseConfigPath);
                    }
                    serviceAccount = new FileInputStream(configFile);
                    logger.info("Loading Firebase config from absolute path");
                } else {
                    ClassPathResource resource = new ClassPathResource(firebaseConfigPath);
                    if (!resource.exists()) {
                        throw new RuntimeException("Firebase service account file not found in classpath: " + firebaseConfigPath);
                    }
                    serviceAccount = resource.getInputStream();
                    logger.info("Loading Firebase config from classpath");
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setConnectTimeout(30000) // 30 seconds
                        .setReadTimeout(60000)    // 60 seconds
                        .build();

                FirebaseApp app = FirebaseApp.initializeApp(options);
                logger.info("Firebase initialized successfully with name: {}", app.getName());

                serviceAccount.close();
                return app;

            } catch (Exception e) {
                logger.error("Firebase initialization failed: ", e);
                throw new RuntimeException("Firebase initialization failed", e);
            }
        } else {
            logger.info("Firebase already initialized");
            return FirebaseApp.getInstance();
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth() throws IOException {
        FirebaseAuth auth = FirebaseAuth.getInstance(firebaseApp());
        logger.info("✅ FirebaseAuth instance created");
        return auth;
    }
}

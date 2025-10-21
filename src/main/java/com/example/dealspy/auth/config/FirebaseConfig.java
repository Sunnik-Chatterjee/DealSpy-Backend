package com.example.dealspy.auth.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.config.path:/etc/secrets/firebase-service-account.json}")
    private String firebaseConfigPath;

    @Bean
    public FirebaseApp firebaseApp() {
        logger.info("========================================");
        logger.info("🔥 FIREBASE INITIALIZATION STARTING");
        logger.info("========================================");

        try {
            logger.info("📂 Config path: {}", firebaseConfigPath);

            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = null;

                // Try external file first (for Render deployment)
                File externalFile = new File(firebaseConfigPath);
                logger.info("🔍 Checking external file: {}", externalFile.getAbsolutePath());
                logger.info("🔍 File exists: {}", externalFile.exists());

                if (externalFile.exists()) {
                    logger.info("✅ Using external file: {}", firebaseConfigPath);
                    serviceAccount = new FileInputStream(externalFile);
                } else {
                    // List files in /etc/secrets for debugging
                    File secretsDir = new File("/etc/secrets");
                    if (secretsDir.exists() && secretsDir.isDirectory()) {
                        logger.info("📁 Files in /etc/secrets:");
                        String[] files = secretsDir.list();
                        if (files != null && files.length > 0) {
                            for (String file : files) {
                                logger.info("  - {}", file);
                            }
                        } else {
                            logger.warn("⚠️ /etc/secrets is empty!");
                        }
                    } else {
                        logger.warn("⚠️ /etc/secrets directory does not exist!");
                    }

                    // Fallback to classpath resource
                    logger.info("⚠️ External file not found, trying classpath: firebase-service-account.json");
                    try {
                        serviceAccount = new ClassPathResource("firebase-service-account.json").getInputStream();
                        logger.info("✅ Found classpath resource");
                    } catch (Exception e) {
                        logger.error("❌ Classpath resource not found either!");
                        throw new RuntimeException("Firebase config file not found in " + firebaseConfigPath + " or classpath", e);
                    }
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp app = FirebaseApp.initializeApp(options);

                logger.info("✅ FIREBASE INITIALIZED SUCCESSFULLY!");
                logger.info("✅ Firebase App Name: {}", app.getName());
                logger.info("✅ Total Apps: {}", FirebaseApp.getApps().size());
                logger.info("========================================");

                return app;

            } else {
                logger.info("✅ Firebase already initialized, returning existing instance");
                return FirebaseApp.getInstance();
            }

        } catch (Exception e) {
            logger.error("========================================");
            logger.error("❌ FIREBASE INITIALIZATION FAILED!");
            logger.error("❌ Error type: {}", e.getClass().getName());
            logger.error("❌ Error message: {}", e.getMessage());
            logger.error("========================================");
            e.printStackTrace();

            // Throw RuntimeException to fail fast - don't let app start without Firebase
            throw new RuntimeException("Failed to initialize Firebase Admin SDK", e);
        }
    }
}

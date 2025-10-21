package com.example.dealspy.auth.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.config.path}")
    private String firebaseConfigPath;

    @PostConstruct
    public void initialize() {
        try (InputStream serviceAccount = new FileInputStream(firebaseConfigPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            System.out.println("✅ Firebase initialized successfully from: " + firebaseConfigPath);
        } catch (FileNotFoundException e) {
            System.err.println("❌ Firebase credentials file NOT FOUND at: " + firebaseConfigPath);
            System.err.println("Available files in /etc/secrets/:");
            File secretsDir = new File("/etc/secrets");
            if (secretsDir.exists() && secretsDir.isDirectory()) {
                String[] files = secretsDir.list();
                if (files != null) {
                    for (String file : files) {
                        System.err.println("  - " + file);
                    }
                }
            } // ✅ Added missing closing brace
            throw new RuntimeException("Firebase initialization failed - credentials not found", e);
        } catch (IOException e) { // ✅ Added missing closing brace
            System.err.println("❌ Failed to read Firebase credentials: " + e.getMessage());
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }
}

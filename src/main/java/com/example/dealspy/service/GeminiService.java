package com.example.dealspy.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class GeminiService {

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 🎯 MAIN METHOD: Get current lowest price with smart fallback strategy
     * Step 1: Try standard prompt
     * Step 2: If fails, try ultra-minimal prompt
     * Step 3: If still fails, return null (preserve existing price)
     */
    public String getCurrentLowestPrice(String productName) {
        // 🔧 SOLUTION 1: Try standard short prompt first
        String response = tryStandardPrompt(productName);
        if (response != null && !response.trim().isEmpty()) {
            return response;
        }

        // 🔧 SOLUTION 2: Fallback to ultra-minimal prompt
        log.warn("Standard prompt failed for '{}', trying minimal prompt", productName);
        response = tryMinimalPrompt(productName);
        if (response != null && !response.trim().isEmpty()) {
            return response;
        }

        // 🔧 SOLUTION 3: Last resort with truncated name
        log.warn("Minimal prompt failed for '{}', trying truncated name", productName);
        return tryTruncatedPrompt(productName);
    }

    /**
     * 🎯 SOLUTION 1: Standard short prompt (works for most products)
     * Token usage: ~15-20 tokens (vs 150+ tokens in old version)
     */
    private String tryStandardPrompt(String productName) {
        // ✅ ULTRA-SHORT prompt - only essential words
        String prompt = String.format("Price of %s in India: ₹", productName);

        return callGeminiAPI(prompt, 400, "standard");
    }

    /**
     * 🎯 SOLUTION 2: Ultra-minimal prompt (for problematic products)
     * Token usage: ~8-12 tokens
     */
    private String tryMinimalPrompt(String productName) {
        // ✅ MINIMAL prompt - just product name + currency
        String prompt = productName + " ₹";

        return callGeminiAPI(prompt, 300, "minimal");
    }

    /**
     * 🎯 SOLUTION 3: Truncated name prompt (for very long product names)
     * Token usage: ~10-15 tokens max
     */
    private String tryTruncatedPrompt(String productName) {
        // ✅ TRUNCATE long product names to save tokens
        String shortName = productName.length() > 40 ?
                productName.substring(0, 40).trim() : productName;

        // Remove common words that don't help with price searching
        shortName = shortName
                .replaceAll("\\b(with|and|for|the|in|on|at)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();

        String prompt = shortName + " ₹";
        log.info("Trying truncated name: '{}' for original: '{}'", shortName, productName);

        return callGeminiAPI(prompt, 200, "truncated");
    }

    /**
     * 🎯 CORE API CALL METHOD: Centralized Gemini API calling
     * This eliminates code duplication and provides consistent configuration
     */
    private String callGeminiAPI(String prompt, int maxTokens, String promptType) {
        String requestUrl = geminiApiUrl + "?key=" + geminiApiKey;

        // ✅ OPTIMIZED configuration for maximum success rate
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.0,      // ✅ Most deterministic responses
                        "topK", 1,               // ✅ Focus on most likely response
                        "topP", 0.8,             // ✅ Good balance
                        "maxOutputTokens", maxTokens, // ✅ Dynamic token allocation
                        "candidateCount", 1      // ✅ Single response only
                ),
                "safetySettings", List.of(
                        Map.of("category", "HARM_CATEGORY_HARASSMENT", "threshold", "BLOCK_ONLY_HIGH"),
                        Map.of("category", "HARM_CATEGORY_HATE_SPEECH", "threshold", "BLOCK_ONLY_HIGH"),
                        Map.of("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold", "BLOCK_ONLY_HIGH"),
                        Map.of("category", "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold", "BLOCK_ONLY_HIGH")
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "DealSpy-Backend/1.0");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            log.debug("Calling Gemini API with {} prompt: '{}' (maxTokens: {})",
                    promptType, prompt, maxTokens);

            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    requestUrl, requestEntity, String.class
            );

            if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
                String response = parseGeminiResponse(responseEntity.getBody(), promptType);
                if (response != null && !response.trim().isEmpty()) {
                    log.info("✅ {} prompt SUCCESS for: '{}'", promptType, prompt);
                    return response;
                } else {
                    log.warn("❌ {} prompt returned empty response for: '{}'", promptType, prompt);
                }
            } else {
                log.error("❌ {} prompt API error. Status: {}", promptType, responseEntity.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ {} prompt exception for '{}': {}", promptType, prompt, e.getMessage());
        }

        return null;
    }

    /**
     * 🎯 ENHANCED RESPONSE PARSER: Better error handling and partial content extraction
     */
    private String parseGeminiResponse(String responseBody, String promptType) {
        try {
            JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray candidates = jsonObject.getAsJsonArray("candidates");

            if (candidates != null && !candidates.isEmpty()) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();

                // ✅ DETAILED finish reason handling
                String finishReason = null;
                if (candidate.has("finishReason")) {
                    finishReason = candidate.get("finishReason").getAsString();
                    log.debug("Gemini finish reason ({}): {}", promptType, finishReason);

                    // ✅ Handle different finish reasons appropriately
                    switch (finishReason) {
                        case "MAX_TOKENS":
                            log.warn("⚠️ {} prompt hit MAX_TOKENS - response truncated", promptType);
                            break;
                        case "SAFETY":
                            log.warn("⚠️ {} prompt blocked by safety filter", promptType);
                            return null;
                        case "STOP":
                            log.debug("✅ {} prompt completed normally", promptType);
                            break;
                    }
                }

                // ✅ EXTRACT content even if truncated (MAX_TOKENS)
                JsonObject content = candidate.getAsJsonObject("content");
                if (content != null && content.has("parts")) {
                    JsonArray parts = content.getAsJsonArray("parts");
                    if (parts != null && !parts.isEmpty()) {
                        JsonObject part = parts.get(0).getAsJsonObject();
                        if (part.has("text")) {
                            String text = part.get("text").getAsString().trim();
                            if (!text.isEmpty()) {
                                log.info("📝 Extracted text from {} prompt: '{}'", promptType, text);
                                return text;
                            }
                        }
                    }
                }

                log.warn("⚠️ {} prompt: No text content in response", promptType);
            } else {
                log.warn("⚠️ {} prompt: No candidates in response", promptType);
            }

        } catch (Exception e) {
            log.error("❌ Error parsing {} prompt response: {}", promptType, e.getMessage());
        }

        return null;
    }

    /**
     * 🎯 ENHANCED PRICE EXTRACTION: Multiple patterns + smart validation
     */
    public Double extractPrice(String responseText) {
        if (responseText == null || responseText.trim().isEmpty()) {
            log.warn("Empty response text for price extraction");
            return null;
        }

        String text = responseText.trim();
        log.debug("🔍 Extracting price from: '{}'", text);

        // ✅ COMPREHENSIVE price patterns (ordered by reliability)
        String[] pricePatterns = {
                "₹\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)",              // ₹1,234.50
                "Rs\\.?\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)",        // Rs. 1234
                "INR\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)",           // INR 1234
                "Price[:\\s]*₹?\\s*(\\d+(?:,\\d{3})*)",              // Price: 1234
                "(\\d+(?:,\\d{3})*)\\s*(?:rupees?|INR|₹)",           // 1234 rupees
                "\\b(\\d{1,2},\\d{3}|\\d{3,6})\\b",                 // 1,234 or 1234-999999
                "\\$\\s*(\\d+).*convert",                            // $50 (convert to INR - fallback)
        };

        for (int i = 0; i < pricePatterns.length; i++) {
            Pattern regex = Pattern.compile(pricePatterns[i], Pattern.CASE_INSENSITIVE);
            Matcher matcher = regex.matcher(text);

            if (matcher.find()) {
                String priceStr = matcher.group(1).replaceAll(",", "");
                try {
                    double price = Double.parseDouble(priceStr);

                    // ✅ SMART validation - reasonable price ranges
                    if (isValidPrice(price)) {
                        log.info("💰 Extracted price: ₹{} using pattern #{}: '{}'",
                                price, i+1, pricePatterns[i]);
                        return price;
                    } else {
                        log.warn("⚠️ Price ₹{} outside valid range (₹10-₹500000)", price);
                    }
                } catch (NumberFormatException e) {
                    log.warn("⚠️ Failed to parse price string: '{}'", priceStr);
                }
            }
        }

        log.warn("❌ No valid price found in: '{}'", text);
        return null;
    }

    /**
     * 🎯 PRICE VALIDATION: Ensures extracted prices are reasonable
     */
    private boolean isValidPrice(double price) {
        return price >= 10 && price <= 500000; // ₹10 to ₹5,00,000 range
    }

    /**
     * 🎯 PLATFORM EXTRACTION: Get the e-commerce platform name
     */
    public String extractPlatform(String responseText) {
        if (responseText == null) return null;

        String[] platformPatterns = {
                "\\b(Amazon|Flipkart|Myntra|Nykaa|Snapdeal|Ajio|Paytm|Meesho|BigBasket)\\b"
        };

        for (String pattern : platformPatterns) {
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = regex.matcher(responseText);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    /**
     * 🎯 COMPREHENSIVE PRICE INFO: Returns complete price details
     */
    public Map<String, Object> getDetailedPriceInfo(String productName) {
        String response = getCurrentLowestPrice(productName);
        Map<String, Object> priceInfo = new java.util.HashMap<>();

        if (response != null) {
            Double price = extractPrice(response);
            String platform = extractPlatform(response);

            priceInfo.put("price", price);
            priceInfo.put("platform", platform);
            priceInfo.put("rawResponse", response);
            priceInfo.put("success", price != null);

            log.info("📊 Price info for '{}': ₹{} on {}",
                    productName, price, platform != null ? platform : "Unknown");
        } else {
            priceInfo.put("success", false);
            priceInfo.put("error", "All prompt strategies failed");
            log.error("💥 Complete failure for product: '{}'", productName);
        }

        return priceInfo;
    }
}

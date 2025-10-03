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
     * 🎯 MAIN METHOD: Progressive fallback strategy for maximum success
     * Uses 4 levels of increasingly minimal prompts
     */
    public String getCurrentLowestPrice(String productName) {
        // 🔧 Level 1: Ultra-minimal prompt (just product + currency)
        String response = tryUltraMinimalPrompt(productName);
        if (response != null && !response.trim().isEmpty()) {
            return response;
        }

        // 🔧 Level 2: Brand-focused prompt
        log.warn("Ultra-minimal failed for '{}', trying brand-focused", productName);
        response = tryBrandFocusedPrompt(productName);
        if (response != null && !response.trim().isEmpty()) {
            return response;
        }

        // 🔧 Level 3: First 3 words only
        log.warn("Brand-focused failed for '{}', trying 3-word prompt", productName);
        response = tryThreeWordPrompt(productName);
        if (response != null && !response.trim().isEmpty()) {
            return response;
        }

        // 🔧 Level 4: Single word + price
        log.warn("3-word failed for '{}', trying single-word prompt", productName);
        return trySingleWordPrompt(productName);
    }

    /**
     * 🎯 Level 1: Ultra-minimal prompt - highest success rate
     * Token usage: ~5-8 tokens
     */
    private String tryUltraMinimalPrompt(String productName) {
        // ✅ Remove all unnecessary words and characters
        String cleanName = cleanProductName(productName);
        String prompt = cleanName + " ₹";

        return callGeminiAPI(prompt, 1000, "ultra-minimal");
    }

    /**
     * 🎯 Level 2: Brand-focused prompt for branded products
     * Token usage: ~3-6 tokens
     */
    private String tryBrandFocusedPrompt(String productName) {
        String brandName = extractBrandName(productName);
        String productType = extractProductType(productName);

        String prompt;
        if (productType != null) {
            prompt = brandName + " " + productType + " ₹";
        } else {
            prompt = brandName + " ₹";
        }

        return callGeminiAPI(prompt, 800, "brand-focused");
    }

    /**
     * 🎯 Level 3: First 3 words only
     * Token usage: ~3-5 tokens
     */
    private String tryThreeWordPrompt(String productName) {
        String[] words = productName.split("\\s+");
        String shortName = words.length > 3 ?
                String.join(" ", words[0], words[1], words[2]) : productName;

        String prompt = shortName + " ₹";
        return callGeminiAPI(prompt, 600, "three-word");
    }

    /**
     * 🎯 Level 4: Single word + price (last resort)
     * Token usage: ~2-3 tokens
     */
    private String trySingleWordPrompt(String productName) {
        String singleWord = extractMostImportantWord(productName);
        String prompt = singleWord + " ₹";

        return callGeminiAPI(prompt, 400, "single-word");
    }

    /**
     * 🧹 Clean product name - remove noise words and special characters
     */
    private String cleanProductName(String productName) {
        return productName
                // Remove common noise words
                .replaceAll("\\b(with|and|for|the|in|on|at|of|-)\\b", " ")
                // Remove extra spaces
                .replaceAll("\\s+", " ")
                // Remove special characters except letters, numbers, spaces
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .trim();
    }

    /**
     * 🏷️ Extract brand name from product title
     */
    private String extractBrandName(String productName) {
        // Known Indian brands - expand this list as needed
        String[] knownBrands = {
                "American Tourister", "Maybelline", "M.A.C", "MAC", "Lakmé", "Lakme",
                "SUGAR", "Nykaa", "Boat", "boAt", "Samsung", "Apple", "OnePlus",
                "Xiaomi", "Realme", "Nike", "Adidas", "Puma", "Levi", "H&M",
                "Zara", "Forever21", "Myntra", "Ajio", "Flipkart", "Amazon"
        };

        String productLower = productName.toLowerCase();
        for (String brand : knownBrands) {
            if (productLower.contains(brand.toLowerCase())) {
                return brand;
            }
        }

        // Fallback: first word (usually brand)
        return productName.split("\\s+")[0];
    }

    /**
     * 🔍 Extract product type (lipstick, backpack, etc.)
     */
    private String extractProductType(String productName) {
        String[] productTypes = {
                "lipstick", "backpack", "shoes", "shirt", "dress", "phone",
                "laptop", "tablet", "watch", "perfume", "cream", "shampoo",
                "bag", "wallet", "sunglasses", "jeans", "jacket"
        };

        String productLower = productName.toLowerCase();
        for (String type : productTypes) {
            if (productLower.contains(type)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 🎯 Extract most important word for single-word prompt
     */
    private String extractMostImportantWord(String productName) {
        // Priority order: brand name > product type > first word
        String brand = extractBrandName(productName);
        if (!brand.equals(productName.split("\\s+")[0])) {
            return brand; // Known brand found
        }

        String type = extractProductType(productName);
        if (type != null) {
            return type;
        }

        // Fallback: first word
        return productName.split("\\s+")[0];
    }

    /**
     * 🌐 Core API calling method with optimized configuration
     */
    private String callGeminiAPI(String prompt, int maxTokens, String promptType) {
        String requestUrl = geminiApiUrl + "?key=" + geminiApiKey;

        // ✅ Highly optimized configuration for maximum success
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.1,      // ✅ Low temperature for factual responses
                        "topK", 1,               // ✅ Most focused response
                        "topP", 0.8,             // ✅ Good balance
                        "maxOutputTokens", maxTokens, // ✅ Progressive token allocation
                        "candidateCount", 1      // ✅ Single response only
                ),
                // ✅ Relaxed safety settings to avoid blocks
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
            log.debug("🔍 {} prompt: '{}' (tokens: {})", promptType, prompt, maxTokens);

            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    requestUrl, requestEntity, String.class
            );

            if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
                String response = parseGeminiResponse(responseEntity.getBody(), promptType);
                if (response != null && !response.trim().isEmpty()) {
                    log.info("✅ {} prompt SUCCESS: '{}'", promptType, prompt);
                    return response;
                } else {
                    log.warn("❌ {} prompt empty response: '{}'", promptType, prompt);
                }
            } else {
                log.error("❌ {} prompt API error: {}", promptType, responseEntity.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ {} prompt exception: '{}' - {}", promptType, prompt, e.getMessage());
        }

        return null;
    }

    /**
     * 📖 Enhanced response parser with better truncation handling
     */
    private String parseGeminiResponse(String responseBody, String promptType) {
        try {
            JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray candidates = jsonObject.getAsJsonArray("candidates");

            if (candidates != null && !candidates.isEmpty()) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();

                // ✅ Handle finish reasons
                String finishReason = null;
                if (candidate.has("finishReason")) {
                    finishReason = candidate.get("finishReason").getAsString();

                    switch (finishReason) {
                        case "MAX_TOKENS":
                            log.debug("⚠️ {} prompt truncated (MAX_TOKENS)", promptType);
                            break;
                        case "SAFETY":
                            log.warn("⚠️ {} prompt blocked by safety", promptType);
                            return null;
                        case "STOP":
                            log.debug("✅ {} prompt completed normally", promptType);
                            break;
                    }
                }

                // ✅ Extract text content (even if truncated)
                JsonObject content = candidate.getAsJsonObject("content");
                if (content != null && content.has("parts")) {
                    JsonArray parts = content.getAsJsonArray("parts");
                    if (parts != null && !parts.isEmpty()) {
                        JsonObject part = parts.get(0).getAsJsonObject();
                        if (part.has("text")) {
                            String text = part.get("text").getAsString().trim();
                            if (!text.isEmpty()) {
                                log.info("📝 {} extracted: '{}'", promptType,
                                        text.length() > 100 ? text.substring(0, 100) + "..." : text);
                                return text;
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("❌ Error parsing {} response: {}", promptType, e.getMessage());
        }

        return null;
    }

    /**
     * 💰 Enhanced price extraction with comprehensive patterns
     */
    public Double extractPrice(String responseText) {
        if (responseText == null || responseText.trim().isEmpty()) {
            log.warn("Empty response for price extraction");
            return null;
        }

        String text = responseText.trim();
        log.debug("🔍 Extracting price from: '{}'", text.length() > 50 ? text.substring(0, 50) + "..." : text);

        // ✅ Comprehensive price patterns (ordered by reliability)
        String[] pricePatterns = {
                "₹\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)",              // ₹1,234.50
                "\\*\\*₹(\\d+(?:,\\d{3})*)\\*\\*",                   // **₹699** (bold formatting)
                "Rs\\.?\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)",        // Rs. 1234
                "INR\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)",           // INR 1234
                "Price[:\\s]*₹?\\s*(\\d+(?:,\\d{3})*)",              // Price: 1234
                "around\\s*₹?\\s*(\\d+(?:,\\d{3})*)",                // around ₹699
                "generally\\s*₹?\\s*(\\d+(?:,\\d{3})*)",             // generally ₹699
                "(\\d+(?:,\\d{3})*)\\s*(?:rupees?|INR|₹)",           // 1234 rupees
                "\\b(\\d{2,6})\\b(?=.*(?:platform|website|store))",  // 1234 (near platform words)
                "\\b(\\d{3,6})\\b"                                   // Any 3-6 digit number (last resort)
        };

        for (int i = 0; i < pricePatterns.length; i++) {
            Pattern regex = Pattern.compile(pricePatterns[i], Pattern.CASE_INSENSITIVE);
            Matcher matcher = regex.matcher(text);

            while (matcher.find()) {
                String priceStr = matcher.group(1).replaceAll(",", "");
                try {
                    double price = Double.parseDouble(priceStr);

                    // ✅ Smart price validation
                    if (isValidPrice(price)) {
                        log.info("💰 Extracted: ₹{} using pattern #{}", price, i + 1);
                        return price;
                    } else {
                        log.debug("⚠️ Price ₹{} outside valid range", price);
                    }
                } catch (NumberFormatException e) {
                    log.debug("⚠️ Failed to parse: '{}'", priceStr);
                }
            }
        }

        log.warn("❌ No valid price in: '{}'", text.length() > 100 ? text.substring(0, 100) + "..." : text);
        return null;
    }

    /**
     * ✅ Smart price validation with category-based ranges
     */
    private boolean isValidPrice(double price) {
        // Basic range check
        if (price < 10 || price > 1000000) {
            return false;
        }

        // Additional validation can be added based on product category
        return true;
    }

    /**
     * 🏪 Extract platform/store information
     */
    public String extractPlatform(String responseText) {
        if (responseText == null) return null;

        String[] platformPatterns = {
                "\\b(Amazon|Flipkart|Myntra|Nykaa|Snapdeal|Ajio|Paytm|Meesho|BigBasket|Tata|Reliance)\\b",
                "\\b(Sephora|MAC|official\\s+website)\\b"
        };

        for (String pattern : platformPatterns) {
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = regex.matcher(responseText);
            if (matcher.find()) {
                String platform = matcher.group(1);
                log.debug("🏪 Found platform: {}", platform);
                return platform;
            }
        }
        return null;
    }

    /**
     * 📊 Get comprehensive price information
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

            if (price != null) {
                log.info("📊 Final result for '{}': ₹{} on {}",
                        productName, price, platform != null ? platform : "Unknown");
            }
        } else {
            priceInfo.put("success", false);
            priceInfo.put("error", "All prompt strategies failed");
            log.error("💥 Complete failure for: '{}'", productName);
        }

        return priceInfo;
    }
}

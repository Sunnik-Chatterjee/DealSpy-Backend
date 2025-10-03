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
     * Get current lowest price for a product using Gemini API
     * @param productName Name of the product
     * @return Raw response text from Gemini
     */
    public String getCurrentLowestPrice(String productName) {
        String enhancedPrompt = String.format(
                "Current price of %s in India? Reply: ₹[amount] on [platform]",
                productName
        );

        String requestUrl = geminiApiUrl + "?key=" + geminiApiKey;

        // Request body for Gemini API
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", enhancedPrompt)
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "topK", 1,
                        "topP", 0.8,
                        "maxOutputTokens", 500,
                        "candidateCount", 1
                ),
                "safetySettings", List.of(
                        Map.of(
                                "category", "HARM_CATEGORY_HARASSMENT",
                                "threshold", "BLOCK_MEDIUM_AND_ABOVE"
                        ),
                        Map.of(
                                "category", "HARM_CATEGORY_HATE_SPEECH",
                                "threshold", "BLOCK_MEDIUM_AND_ABOVE"
                        ),
                        Map.of(
                                "category", "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                                "threshold", "BLOCK_MEDIUM_AND_ABOVE"
                        ),
                        Map.of(
                                "category", "HARM_CATEGORY_DANGEROUS_CONTENT",
                                "threshold", "BLOCK_MEDIUM_AND_ABOVE"
                        )
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "DealSpy-Backend/1.0");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            log.info("Searching price for product: {}", productName);

            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    requestUrl, requestEntity, String.class
            );

            if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
                String rawResponse = responseEntity.getBody();
                log.debug("Gemini API Response for '{}': {}", productName, rawResponse);
                return parseGeminiResponse(rawResponse);
            } else {
                log.error("Failed to call Gemini API. Status: {}", responseEntity.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            log.error("Error calling Gemini API for product '{}': {}", productName, e.getMessage());
            return null;
        }
    }

    /**
     * Parse the Gemini API response to extract the text content
     * @param responseBody Raw JSON response from Gemini API
     * @return Extracted text content
     */
    private String parseGeminiResponse(String responseBody) {
        try {
            JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray candidates = jsonObject.getAsJsonArray("candidates");

            if (candidates != null && !candidates.isEmpty()) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();

                if (candidate.has("finishReason")) {
                    String finishReason = candidate.get("finishReason").getAsString();
                    if ("MAX_TOKENS".equals(finishReason)) {
                        log.warn("Gemini response was cut off due to MAX_TOKENS limit");
                    } else if ("SAFETY".equals(finishReason)) {
                        log.warn("Gemini response blocked due to safety settings");
                        return null;
                    }
                    log.debug("Gemini finish reason: {}", finishReason);
                }

                JsonObject content = candidate.getAsJsonObject("content");
                if (content != null && content.has("parts")) {
                    JsonArray parts = content.getAsJsonArray("parts");
                    if (parts != null && !parts.isEmpty()) {
                        JsonObject part = parts.get(0).getAsJsonObject();
                        if (part.has("text")) {
                            String text = part.get("text").getAsString();
                            log.info("Extracted text from Gemini: '{}'", text);
                            return text;
                        }
                    }
                }
                log.warn("No text content found in Gemini response for candidate");
                return null;
            }

            log.warn("No candidates found in Gemini response");
            return null;

        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extract price from the response text using multiple regex patterns
     * @param responseText Text response from Gemini
     * @return Extracted price as Double, or null if no price found
     */
    public Double extractPrice(String responseText) {
        if (responseText == null || responseText.trim().isEmpty()) {
            log.warn("Empty response text provided for price extraction");
            return null;
        }

        log.debug("Extracting price from text: '{}'", responseText);

        // ✅ Multiple regex patterns to catch different price formats
        String[] pricePatterns = {
                "₹\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)",              // ₹1,234.50
                "INR\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)",           // INR 1234.50
                "Rs\\.?\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)",        // Rs. 1234.50
                "Price[:\\s]*₹?\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)", // Price: ₹1234
                "Lowest.*?Price.*?₹?\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)", // Lowest Price: 1234
                "(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)\\s*(?:rupees?|INR|₹)", // 1234 rupees
                "\\b(\\d{1,2},\\d{3}|\\d{1,5})\\b.*?(?:rupees?|INR|₹|on\\s+\\w+)", // 1,234 on Amazon
                "\\b(\\d{3,6})\\b(?=.*(?:Amazon|Flipkart|Myntra|Nykaa|Snapdeal))" // 1234 (followed by platform)
        };

        for (String pattern : pricePatterns) {
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = regex.matcher(responseText);

            if (matcher.find()) {
                String priceStr = matcher.group(1).replaceAll(",", "");
                try {
                    double price = Double.parseDouble(priceStr);

                    // ✅ Sanity check - reasonable price range
                    if (price >= 10 && price <= 1000000) {
                        log.info("Extracted price: ₹{} using pattern: '{}'", price, pattern);
                        return price;
                    } else {
                        log.warn("Price out of reasonable range: ₹{}", price);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse price: '{}'", priceStr);
                }
            }
        }

        // ✅ Last resort: extract first number that looks like a price
        Pattern fallbackPattern = Pattern.compile("\\b(\\d{2,6})\\b");
        Matcher fallbackMatcher = fallbackPattern.matcher(responseText);

        while (fallbackMatcher.find()) {
            try {
                double price = Double.parseDouble(fallbackMatcher.group(1));
                if (price >= 50 && price <= 500000) { // More restrictive for fallback
                    log.info("Fallback price extraction: ₹{}", price);
                    return price;
                }
            } catch (NumberFormatException e) {
                // Continue to next match
            }
        }

        log.warn("No valid price found in response: '{}'", responseText);
        return null;
    }

    /**
     * Extract platform information from the response text
     * @param responseText Text response from Gemini
     * @return Platform name or null if not found
     */
    public String extractPlatform(String responseText) {
        if (responseText == null || responseText.trim().isEmpty()) {
            return null;
        }

        // ✅ Platform name patterns
        String[] platformPatterns = {
                "on\\s+(Amazon|Flipkart|Snapdeal|Myntra|Ajio|Paytm|BigBasket|Nykaa|Meesho)",
                "available\\s+on\\s+(Amazon|Flipkart|Snapdeal|Myntra|Ajio|Paytm|BigBasket|Nykaa|Meesho)",
                "from\\s+(Amazon|Flipkart|Snapdeal|Myntra|Ajio|Paytm|BigBasket|Nykaa|Meesho)",
                "\\b(Amazon|Flipkart|Snapdeal|Myntra|Ajio|Paytm|BigBasket|Nykaa|Meesho)\\b"
        };

        for (String pattern : platformPatterns) {
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = regex.matcher(responseText);
            if (matcher.find()) {
                String platform = matcher.group(1);
                log.debug("Extracted platform: {}", platform);
                return platform;
            }
        }

        log.debug("No platform found in response: '{}'", responseText);
        return null;
    }

    /**
     * Get detailed price information including platform and offers
     * @param productName Name of the product
     * @return Map containing price, platform, and other details
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
        } else {
            priceInfo.put("success", false);
            priceInfo.put("error", "Failed to get response from Gemini API");
        }

        return priceInfo;
    }

    /**
     * Alternative method with ultra-simple prompt for problematic products
     * @param productName Name of the product
     * @return Raw response text from Gemini
     */
    public String getSimplePrice(String productName) {
        String simplePrompt = productName + " price India ₹";

        String requestUrl = geminiApiUrl + "?key=" + geminiApiKey;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", simplePrompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.0,
                        "maxOutputTokens", 100
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            log.info("Trying simple price search for: {}", productName);

            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    requestUrl, requestEntity, String.class
            );

            if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
                return parseGeminiResponse(responseEntity.getBody());
            }
        } catch (Exception e) {
            log.error("Simple price search failed for '{}': {}", productName, e.getMessage());
        }

        return null;
    }
}

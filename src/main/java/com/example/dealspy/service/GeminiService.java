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
     * Search for the lowest price across multiple e-commerce platforms using Gemini 2.5 Flash
     * @param productName The product name to search for
     * @return Parsed response from Gemini containing price information
     */
    public String getCurrentLowestPrice(String productName) {
        // Enhanced prompt for better accuracy and multi-platform search
        String enhancedPrompt = String.format(
                "Find the absolute lowest current price for the product '%s' by searching across these Indian e-commerce platforms: " +
                        "Amazon India, Flipkart, Snapdeal, Myntra, Ajio, Paytm Mall, BigBasket, Nykaa, and other major retailers. " +
                        "Please provide:\n" +
                        "1. The lowest price in INR\n" +
                        "2. Platform name where this lowest price is found\n" +
                        "3. Any current offers or discounts\n" +
                        "Format: 'Lowest Price: ₹[amount] on [platform]'\n" +
                        "Product: %s",
                productName, productName
        );

        String requestUrl = geminiApiUrl + "?key=" + geminiApiKey;

        // Request body for Gemini 2.5 Flash API
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", enhancedPrompt)
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "temperature", 0.1,  // Lower temperature for more factual responses
                        "topK", 1,
                        "topP", 0.8,
                        "maxOutputTokens", 200
                ),
                "safetySettings", List.of(
                        Map.of(
                                "category", "HARM_CATEGORY_HARASSMENT",
                                "threshold", "BLOCK_MEDIUM_AND_ABOVE"
                        ),
                        Map.of(
                                "category", "HARM_CATEGORY_HATE_SPEECH",
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
                log.info("Gemini API Response for '{}': {}", productName, rawResponse);
                return parseGeminiResponse(rawResponse);
            } else {
                log.error("Failed to call Gemini API. Status: {}", responseEntity.getStatusCode());
                throw new RuntimeException("Failed to call Gemini API: " + responseEntity.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error calling Gemini API for product '{}': {}", productName, e.getMessage(), e);
            throw new RuntimeException("Error while calling Gemini API: " + e.getMessage());
        }
    }

    /**
     * Parse the Gemini API response to extract the text content
     */
    private String parseGeminiResponse(String responseBody) {
        try {
            JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray candidates = jsonObject.getAsJsonArray("candidates");

            if (candidates != null && !candidates.isEmpty()) {
                JsonObject content = candidates.get(0).getAsJsonObject()
                        .getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");

                if (parts != null && !parts.isEmpty()) {
                    return parts.get(0).getAsJsonObject().get("text").getAsString();
                }
            }

            log.warn("No valid content found in Gemini response");
            return "";

        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * Extract the lowest price from Gemini response text using improved regex patterns
     */
    public Double extractPrice(String responseText) {
        if (responseText == null || responseText.trim().isEmpty()) {
            log.warn("Empty response text provided for price extraction");
            return 0.0;
        }

        // Multiple regex patterns to catch different price formats
        String[] pricePatterns = {
                "₹\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)",           // ₹1,234.50
                "INR\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)",        // INR 1234.50
                "Rs\\.?\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)",     // Rs. 1234.50
                "Price:\\s*₹?\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)", // Price: ₹1234
                "Lowest.*?Price.*?₹?\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)", // Lowest Price: 1234
                "(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)\\s*(?:rupees?|INR|₹)" // 1234 rupees
        };

        for (String pattern : pricePatterns) {
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = regex.matcher(responseText);

            if (matcher.find()) {
                String priceStr = matcher.group(1).replaceAll(",", "");
                try {
                    double price = Double.parseDouble(priceStr);
                    log.info("Extracted price: ₹{} from response: {}", price, responseText.substring(0, Math.min(100, responseText.length())));
                    return price;
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse price: {}", priceStr);
                    continue;
                }
            }
        }

        // Fallback: extract any number and assume it's the price
        Pattern fallbackPattern = Pattern.compile("(\\d+(?:\\.\\d{2})?)");
        Matcher fallbackMatcher = fallbackPattern.matcher(responseText);
        if (fallbackMatcher.find()) {
            try {
                double price = Double.parseDouble(fallbackMatcher.group(1));
                log.info("Fallback extraction: ₹{}", price);
                return price;
            } catch (NumberFormatException e) {
                log.error("Failed to parse fallback price");
            }
        }

        log.warn("No price found in response: {}", responseText);
        return 0.0;
    }

    /**
     * Extract additional information like platform name and offers from the response
     */
    public Map<String, String> extractDetailedInfo(String responseText) {
        Map<String, String> info = new java.util.HashMap<>();

        // Extract platform name
        String[] platformPatterns = {
                "on (Amazon|Flipkart|Snapdeal|Myntra|Ajio|Paytm|BigBasket|Nykaa)",
                "available on (Amazon|Flipkart|Snapdeal|Myntra|Ajio|Paytm|BigBasket|Nykaa)",
                "from (Amazon|Flipkart|Snapdeal|Myntra|Ajio|Paytm|BigBasket|Nykaa)"
        };

        for (String pattern : platformPatterns) {
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = regex.matcher(responseText);
            if (matcher.find()) {
                info.put("platform", matcher.group(1));
                break;
            }
        }

        // Extract offers/discounts
        Pattern offerPattern = Pattern.compile("(\\d+%\\s*off|discount|offer|sale)", Pattern.CASE_INSENSITIVE);
        Matcher offerMatcher = offerPattern.matcher(responseText);
        if (offerMatcher.find()) {
            info.put("offer", offerMatcher.group());
        }

        return info;
    }
}

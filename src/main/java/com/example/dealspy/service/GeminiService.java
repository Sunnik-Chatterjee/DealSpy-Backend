package com.example.dealspy.service;

import com.example.dealspy.model.Product;
import com.example.dealspy.repo.ProductRepo;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private ProductRepo productRepo;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();


    public void updateAllProductPricesAndDeepLinks() {
        log.info("Starting comprehensive product price and deep link update using Gemini web search...");

        List<Product> allProducts = productRepo.findAll();
        int successCount = 0;
        int failureCount = 0;

        for (Product product : allProducts) {
            try {
                log.info("Processing product: {}", product.getName());
                PriceSearchResult result = searchLowestPriceWithDeepLink(product.getName());

                if (result != null && result.isSuccess()) {
                    Double newPrice = result.getLowestPrice();
                    String deepLink = result.getDeepLink();
                    product.setCurrentPrice(newPrice);
                    product.setDeepLink(deepLink);
                    if (product.getLastLowestPrice() != null && newPrice < product.getLastLowestPrice()) {
                        product.setIsPriceDropped(true);
                        product.setLastLowestPrice(newPrice);
                    } else if (product.getLastLowestPrice() == null) {
                        product.setLastLowestPrice(newPrice);
                        product.setIsPriceDropped(false);
                    }

                    productRepo.save(product);
                    successCount++;

                    log.info("Updated {}: ₹{} from {} with deep link",
                            product.getName(), newPrice, result.getPlatform());

                } else {
                    failureCount++;
                    log.warn("Failed to get price info for: {}", product.getName());
                }
                Thread.sleep(1500);
            } catch (Exception e) {
                failureCount++;
                log.error("Error processing {}: {}", product.getName(), e.getMessage());
            }
        }
        log.info("Update complete: {} success, {} failures", successCount, failureCount);
    }

    PriceSearchResult searchLowestPriceWithDeepLink(String productName) {
        String prompt = createECommerceSearchPrompt(productName);

        String response = callGeminiForWebSearch(prompt);
        if (response != null) {
            return parseWebSearchResponse(response);
        }
        return null;
    }

    private String createECommerceSearchPrompt(String productName) {
        String cleanName = cleanProductName(productName);

        return String.format("find lowest current price %s online shopping Flipkart Amazon Myntra Nykaa Ajio Blinkit Mamaearth Shopsy with buy link ", cleanName);
    }

    private String cleanProductName(String productName) {
        return productName
                .replaceAll("\\b(with|and|for|the|in|on|at|of|by|from)\\b", "")
                .replaceAll("[^a-zA-Z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String callGeminiForWebSearch(String prompt) {
        String requestUrl = geminiApiUrl + "?key=" + geminiApiKey;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.1,  // Low temperature for factual web search results
                        "topK", 1,
                        "topP", 0.8,
                        "maxOutputTokens", 400, // Enough for price + platform + link
                        "candidateCount", 1
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
        headers.set("User-Agent", "DealSpy-WebSearch/1.0");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            log.debug("Web search prompt: '{}'", prompt);

            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    requestUrl, requestEntity, String.class
            );

            if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
                return extractGeminiResponse(responseEntity.getBody());
            }

        } catch (Exception e) {
            log.error("Web search API call failed: {}", e.getMessage());
        }

        return null;
    }

    private String extractGeminiResponse(String responseBody) {
        try {
            JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray candidates = jsonObject.getAsJsonArray("candidates");

            if (candidates != null && !candidates.isEmpty()) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();
                JsonObject content = candidate.getAsJsonObject("content");

                if (content != null && content.has("parts")) {
                    JsonArray parts = content.getAsJsonArray("parts");
                    if (!parts.isEmpty()) {
                        JsonObject part = parts.get(0).getAsJsonObject();
                        if (part.has("text")) {
                            return part.get("text").getAsString().trim();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage());
        }
        return null;
    }

    private PriceSearchResult parseWebSearchResponse(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }
        log.debug("Parsing web search response: {}", response.length() > 100 ? response.substring(0, 100) + "..." : response);
        Double price = extractBestPrice(response);
        String platform = extractBestPlatform(response);
        String deepLink = extractDeepLink(response);
        if (price != null) {
            return new PriceSearchResult(price, platform, deepLink, true);
        }
        return new PriceSearchResult(null, null, null, false);
    }

    private Double extractBestPrice(String response) {
        String[] pricePatterns = {
                // E-commerce specific patterns
                "₹\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)",
                "(Flipkart|Amazon|Myntra|Nykaa|Ajio|Blinkit|Mamaearth|Shopsy)\\s*:?\\s*₹\\s*(\\d+(?:,\\d{3})*)", // Platform: ₹price
                "₹\\s*(\\d+(?:,\\d{3})*)\\s*(?:on|at)\\s*(Flipkart|Amazon|Myntra|Nykaa|Ajio|Blinkit|Mamaearth|Shopsy)", // ₹price on Platform
                "lowest\\s*price\\s*₹\\s*(\\d+(?:,\\d{3})*)",
                "best\\s*price\\s*₹\\s*(\\d+(?:,\\d{3})*)",
                "Rs\\.?\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)",
                "INR\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)",
                "\\b(\\d{3,6})\\b(?=.*(?:rupees?|₹|price))"
        };

        Double lowestPrice = null;

        for (String pattern : pricePatterns) {
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = regex.matcher(response);

            while (matcher.find()) {
                try {
                    String priceStr;
                    if (pattern.contains("(Flipkart|Amazon")) {
                        priceStr = matcher.group(2);
                    } else if (pattern.contains("on|at")) {
                        priceStr = matcher.group(1);
                    } else {
                        priceStr = matcher.group(1);
                    }

                    priceStr = priceStr.replaceAll(",", "");
                    double price = Double.parseDouble(priceStr);

                    if (isValidPrice(price)) {
                        if (lowestPrice == null || price < lowestPrice) {
                            lowestPrice = price;
                        }
                    }
                } catch (NumberFormatException | IndexOutOfBoundsException ignored) {
                }
            }
        }

        if (lowestPrice != null) {
            log.info("Found lowest price: ₹{}", lowestPrice);
        }

        return lowestPrice;
    }

    private String extractBestPlatform(String response) {
        String[] platforms = {
                "Flipkart", "Amazon", "Myntra", "Nykaa", "Ajio", "Blinkit",
                "Mamaearth", "Shopsy", "Snapdeal", "Paytm", "Meesho",
                "BigBasket", "Tata CLiQ", "Reliance Digital"
        };
        for (String platform : platforms) {
            if (response.toLowerCase().contains(platform.toLowerCase())) {
                log.debug("Found platform: {}", platform);
                return platform;
            }
        }
        return "Unknown";
    }

    private String extractDeepLink(String response) {
        if (response == null || response.trim().isEmpty()) {
            log.warn("Empty response received for deeplink extraction");
            return null;
        }

        log.debug("Extracting deeplink from response: {}", response.length() > 100 ? response.substring(0, 100) + "..." : response);

        String[] urlPatterns = {
                "https?://(?:www\\.)?flipkart\\.com[^\\s]*",
                "https?://dl\\.flipkart\\.com[^\\s]*",

                "https?://(?:www\\.)?amazon\\.in[^\\s]*",
                "https?://amzn\\.to[^\\s]*",

                "https?://(?:www\\.)?myntra\\.com[^\\s]*",
                "https?://(?:www\\.)?ajio\\.com[^\\s]*",

                "https?://(?:www\\.)?nykaa\\.com[^\\s]*",
                "https?://(?:www\\.)?mamaearth\\.in[^\\s]*",

                "https?://(?:www\\.)?blinkit\\.com[^\\s]*",
                "https?://(?:www\\.)?bigbasket\\.com[^\\s]*",
                "https?://(?:www\\.)?grofers\\.com[^\\s]*",
                "https?://(?:www\\.)?jiomart\\.com[^\\s]*",

                "https?://(?:www\\.)?shopsy\\.in[^\\s]*",
                "https?://(?:www\\.)?snapdeal\\.com[^\\s]*",
                "https?://(?:www\\.)?paytmmall\\.com[^\\s]*",
                "https?://(?:www\\.)?meesho\\.com[^\\s]*",
                "https?://(?:www\\.)?tatacliq\\.com[^\\s]*",
                "https?://(?:www\\.)?reliancedigital\\.in[^\\s]*",
                "https?://(?:www\\.)?croma\\.com[^\\s]*",

                "https?://[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,}[^\\s]*(?:product|item|buy|shop|deal)[^\\s]*"
        };

        for (String pattern : urlPatterns) {
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = regex.matcher(response);

            if (matcher.find()) {
                String url = matcher.group().trim();

                url = url.replaceAll("[.,!?;]+$", "");

                if (isValidECommerceUrl(url)) {
                    String cleanedUrl = cleanUrl(url);
                    log.info("🔗 Found deep link from {}: {}",
                            extractDomainFromUrl(cleanedUrl), cleanedUrl);
                    return cleanedUrl;
                } else {
                    log.debug("Invalid e-commerce URL found: {}", url);
                }
            }
        }

        log.warn("No valid deeplink found in response");
        return null;
    }

    private boolean isValidECommerceUrl(String url) {
        if (url == null || url.length() < 15) {
            return false;
        }

        String urlLower = url.toLowerCase();

        String[] validDomains = {
                "flipkart.com", "dl.flipkart.com",
                "amazon.in", "amzn.to",
                "myntra.com", "ajio.com",
                "nykaa.com", "mamaearth.in",
                "blinkit.com", "bigbasket.com", "grofers.com", "jiomart.com",
                "shopsy.in", "snapdeal.com", "paytmmall.com",
                "meesho.com", "tatacliq.com", "reliancedigital.in", "croma.com"
        };

        for (String domain : validDomains) {
            if (urlLower.contains(domain)) {
                return true;
            }
        }

        return urlLower.matches(".*\\b(product|item|buy|shop|deal|p-|dp/)\\b.*") &&
                urlLower.matches("https?://[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,}/.*");
    }

    private String cleanUrl(String url) {
        if (url == null) {
            return null;
        }

        try {
            return url
                    // Remove UTM and tracking parameters
                    .replaceAll("[?&](utm_[^&]*|ref[^&]*|tag[^&]*|campaign[^&]*|source[^&]*|medium[^&]*)", "")
                    .replaceAll("[?&](gclid[^&]*|fbclid[^&]*|msclkid[^&]*)", "")
                    .replaceAll("[?&](pid[^&]*|affid[^&]*|pf_rd_[^&]*)", "")
                    // Clean up multiple ? or &
                    .replaceAll("\\?&", "?")
                    .replaceAll("&&+", "&")
                    // Remove trailing ? or &
                    .replaceAll("[?&]$", "")
                    // Ensure proper URL formatting
                    .trim();
        } catch (Exception e) {
            log.warn("Error cleaning URL {}: {}", url, e.getMessage());
            return url;
        }
    }

    private String extractDomainFromUrl(String url) {
        if (url == null) return "Unknown";

        try {
            Pattern domainPattern = Pattern.compile("https?://(?:www\\.)?([^/]+)");
            Matcher matcher = domainPattern.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            log.debug("Could not extract domain from URL: {}", url);
        }

        return "Unknown";
    }

    private boolean isValidPrice(double price) {
        return price >= 10 && price <= 1000000;
    }


    @Getter
    public static class PriceSearchResult {
        private final Double lowestPrice;
        private final String platform;
        private final String deepLink;
        private final boolean success;

        public PriceSearchResult(Double lowestPrice, String platform, String deepLink, boolean success) {
            this.lowestPrice = lowestPrice;
            this.platform = platform;
            this.deepLink = deepLink;
            this.success = success;
        }

    }
}

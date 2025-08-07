package com.example.dealspy.service;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getCurrentLowestPrice(String productName) {
        //TODO: Improve the prompt for more accurate results
        String prompt = "What is the current lowest price of the product: " + productName;
        String requestUrl = geminiApiUrl + "?key=" + geminiApiKey;

        // Construct request body
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(requestUrl, requestEntity, String.class);

            if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
                String rawResponse = responseEntity.getBody();
                System.out.println("Gemini Raw Response: " + rawResponse);
                return parseGeminiResponse(rawResponse);
            } else {
                throw new RuntimeException("Failed to call Gemini API: " + responseEntity.getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error while calling Gemini API: " + e.getMessage());
        }
    }

    private String parseGeminiResponse(String responseBody) {
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
        return "";
    }

    public double extractPrice(String responseText) {
        // Extract digits from response text like: "The lowest price of this product is: 12344"
        String priceStr = responseText.replaceAll("\\D+", "");
        return priceStr.isEmpty() ? 0.0 : Double.parseDouble(priceStr);
    }
}

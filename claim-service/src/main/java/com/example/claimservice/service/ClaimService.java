package com.example.claimservice.service;

import com.example.claimservice.client.PricingClient;
import com.example.claimservice.entity.Claim;
import com.example.claimservice.repository.ClaimRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository repo;
    private final PricingClient pricingClient;
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    public Claim createClaim(String username, String photoUrl) {
        // 1. Gemini API-ni çağırırıq
        String geminiResponseJson;
        try {
            geminiResponseJson = callGemini(photoUrl);
        } catch (Exception e) {
            log.warn("Gemini call failed for photoUrl={}: {}", photoUrl, e.getMessage());
            geminiResponseJson = "{\"error\": \"Gemini call failed: " + e.getMessage().replace("\"", "'") + "\"}";
        }

        // 2. Gemini-dən gələn mətndən zədəli detalları ayırırıq
        List<String> damagedParts = extractDamagedParts(geminiResponseJson);

        // 3. Hər detal üçün Pricing-service-dən qiymət soruşuruq
        double totalCost = 0.0;
        for (String part : damagedParts) {
            try {
                // FeignClient vasitəsilə digər servisə müraciət
                double price = pricingClient.getPrice(part);
                totalCost += price;
            } catch (Exception ex) {
                // Əgər qiymət tapılmasa (404 və s.), 0 qəbul edib davam edirik
                System.out.println("Price not found for part: " + part);
            }
        }

        // 4. Məlumatları PostgreSQL-ə yazırıq
        Claim claim = Claim.builder()
                .username(username)
                .photoUrl(photoUrl)
                .damagedParts(damagedParts)
                .totalCost(totalCost)
                .jsonData(geminiResponseJson) // Gələcəkdə analiz üçün tam JSON-u saxlayırıq
                .createdAt(LocalDateTime.now())
                .build();

        return repo.save(claim);
    }

    public List<Claim> getAllClaims() {
        return repo.findAll();
    }

    private String callGemini(String photoUrl) {
        // 1. Fetch image from URL and encode as base64 (Gemini requires inline image data, not URL)
        byte[] imageBytes;
        try {
            imageBytes = webClient.get()
                    .uri(photoUrl)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
        } catch (Exception e) {
            log.warn("Failed to fetch image from URL, falling back to text-only prompt: {}", e.getMessage());
            // Fallback: ask Gemini with URL in text (may not analyze image but avoids hard failure)
            return callGeminiTextOnly(photoUrl);
        }
        if (imageBytes == null || imageBytes.length == 0) {
            return callGeminiTextOnly(photoUrl);
        }
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String mimeType = inferMimeType(photoUrl);

        // 2. Build request with inlineData (Gemini API format)
        List<Map<String, Object>> parts = List.of(
                Map.of(
                        "inlineData", Map.of(
                                "mimeType", mimeType,
                                "data", base64Image
                        )
                ),
                Map.of("text", "Identify damaged car parts in this image. List only the part names, one per line (e.g. Hood, Front Bumper, Fender). Use common English names.")
        );
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", parts))
        );

        return webClient.post()
                .uri(geminiApiUrl + "?key=" + geminiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private String callGeminiTextOnly(String photoUrl) {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", "Based on this image URL, list possible damaged car parts (one per line): " + photoUrl + ". Use names like: Hood, Front Bumper, Fender, Door.")
                        ))
                )
        );
        return webClient.post()
                .uri(geminiApiUrl + "?key=" + geminiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private static String inferMimeType(String url) {
        if (url == null) return "image/jpeg";
        String lower = url.toLowerCase();
        if (lower.contains(".png")) return "image/png";
        if (lower.contains(".webp")) return "image/webp";
        if (lower.contains(".gif")) return "image/gif";
        return "image/jpeg";
    }

    private List<String> extractDamagedParts(String geminiJson) {
        List<String> parts = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(geminiJson);
            if (root.has("error")) {
                log.debug("Gemini returned error: {}", root.path("error").asText());
                return parts;
            }
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                return parts;
            }
            JsonNode content = candidates.get(0).path("content");
            JsonNode partsNode = content.path("parts");
            if (!partsNode.isArray() || partsNode.isEmpty()) {
                return parts;
            }
            JsonNode textNode = partsNode.get(0).path("text");
            if (textNode.isMissingNode() || !textNode.isTextual()) {
                return parts;
            }
            String text = textNode.asText();
            String[] lines = text.split("\\n");
            for (String line : lines) {
                String clean = line.replaceAll("[^a-zA-Z ]", "").trim();
                if (!clean.isEmpty()) {
                    parts.add(clean);
                }
            }
        } catch (Exception e) {
            log.warn("JSON parsing error extracting damaged parts: {}", e.getMessage());
        }
        return parts;
    }
}
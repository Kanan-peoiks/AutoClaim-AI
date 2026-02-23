package com.example.claimservice.service;

import com.example.claimservice.client.PricingClient;
import com.example.claimservice.entity.Claim;
import com.example.claimservice.repository.ClaimRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
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

    private URI buildGeminiUri() {
        return UriComponentsBuilder.fromUriString(geminiApiUrl)
                .queryParam("key", geminiApiKey)
                .build()
                .toUri();
    }

    private static final String GEMINI_PROMPT = "Analyze this car damage. Return ONLY a comma-separated list of damaged parts found in this list: [Headlight, Bumper, Hood, Door, Fender]. If no damage, return 'None'.";

    private String callGemini(String photoUrl) {
        // 1. Fetch image from URL (follow redirects; 404/5xx fall back to text-only)
        byte[] imageBytes = null;
        try {
            imageBytes = webClient.get()
                    .uri(photoUrl)
                    .header("User-Agent", "AutoClaim-AI/1.0 (Spring WebClient)")
                    .retrieve()
                    .onStatus(org.springframework.http.HttpStatusCode::isError, resp -> {
                        log.warn("Image fetch failed: {} for URL: {}", resp.statusCode(), photoUrl);
                        return resp.createException();
                    })
                    .bodyToMono(byte[].class)
                    .block();
        } catch (Exception e) {
            log.warn("Failed to fetch image from URL, falling back to text-only prompt: {}", e.getMessage());
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
                Map.of("text", GEMINI_PROMPT)
        );
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", parts))
        );

        return webClient.post()
                .uri(buildGeminiUri())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private String callGeminiTextOnly(String photoUrl) {
        String textPrompt = "Analyze this car damage. Return ONLY a comma-separated list of damaged parts found in this list: [Headlight, Bumper, Hood, Door, Fender]. If no damage, return 'None'. Image URL for context: " + photoUrl;
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", textPrompt)))
                )
        );
        return webClient.post()
                .uri(buildGeminiUri())
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
            String text = textNode.asText().trim();
            if ("None".equalsIgnoreCase(text)) {
                return parts;
            }
            // Support comma-separated list and newline-separated list
            String[] tokens = text.split("[,\\n]");
            for (String token : tokens) {
                String clean = token.replaceAll("[^a-zA-Z ]", "").trim();
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
package com.example.claimservice.service;

import com.example.claimservice.client.PricingClient;
import com.example.claimservice.entity.Claim;
import com.example.claimservice.repository.ClaimRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            // Xəta olarsa boş JSON qaytarırıq ki, sistem çökməsin
            geminiResponseJson = "{\"error\": \"Gemini call failed\"}";
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
        // Gemini-yə göndərilən sorğu strukturu
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", "Identify damaged car parts in this image URL: " + photoUrl +
                                        ". Provide the names of parts separated by new lines only.")
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

    private List<String> extractDamagedParts(String geminiJson) {
        List<String> parts = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(geminiJson);
            // Gemini-nin rəsmi cavab yolu: candidates[0] -> content -> parts[0] -> text
            JsonNode textNode = root.path("candidates").get(0)
                    .path("content").path("parts").get(0).path("text");

            if (textNode != null && !textNode.isMissingNode()) {
                String text = textNode.asText();
                // Mətni sətirlərə bölürük və hər sətirdəki detal adını təmizləyirik
                String[] lines = text.split("\\n");
                for (String line : lines) {
                    // Yalnız hərfləri və boşluqları saxlayırıq (məs: "Front Bumper")
                    String clean = line.replaceAll("[^a-zA-Z ]", "").trim();
                    if (!clean.isEmpty()) {
                        parts.add(clean);
                    }
                }
            }
        } catch (Exception e) {
            // Parsing xətası olarsa boş siyahı qaytarırıq
            System.err.println("JSON Parsing error: " + e.getMessage());
        }
        return parts;
    }
}
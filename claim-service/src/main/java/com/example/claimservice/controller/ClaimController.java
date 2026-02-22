package com.example.claimservice.controller;

import com.example.claimservice.dto.CreateClaimRequest;
import com.example.claimservice.entity.Claim;
import com.example.claimservice.service.ClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping("/create")
    public ResponseEntity<?> createClaim(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String photoUrl,
            @RequestBody(required = false) CreateClaimRequest body) {

        if (body != null && body.getUsername() != null && body.getPhotoUrl() != null) {
            username = body.getUsername();
            photoUrl = body.getPhotoUrl();
        }
        if (username == null || username.isBlank() || photoUrl == null || photoUrl.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("username and photoUrl are required (query params or JSON body)");
        }
        Claim c = claimService.createClaim(username, photoUrl);
        return ResponseEntity.ok(c);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Claim>> getAll() {
        return ResponseEntity.ok(claimService.getAllClaims());
    }
}


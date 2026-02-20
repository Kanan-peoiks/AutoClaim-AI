package com.example.claimservice.controller;

import com.example.claimservice.entity.Claim;
import com.example.claimservice.service.ClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping("/create")
    public ResponseEntity<Claim> createClaim(
            @RequestParam String username,
            @RequestParam String photoUrl) {

        Claim c = claimService.createClaim(username, photoUrl);
        return ResponseEntity.ok(c);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Claim>> getAll() {
        return ResponseEntity.ok(claimService.getAllClaims());
    }
}


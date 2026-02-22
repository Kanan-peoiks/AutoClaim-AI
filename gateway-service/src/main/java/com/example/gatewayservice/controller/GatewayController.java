package com.example.gatewayservice.controller;

import com.example.gatewayservice.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/gateway")
@RequiredArgsConstructor
public class GatewayController {

    private final JwtUtil jwtUtil;
    private final ConcurrentHashMap<String, Integer> userCounter = new ConcurrentHashMap<>();

    @GetMapping("/check-limit")
    public ResponseEntity<String> checkLimit(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token missing");
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        String username = jwtUtil.getUsername(token);
        userCounter.putIfAbsent(username, 0);

        if (userCounter.get(username) >= 3) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Limit exceeded");
        }

        userCounter.put(username, userCounter.get(username) + 1);
        return ResponseEntity.ok("Allowed. Calls left: " + (3 - userCounter.get(username)));
    }
}
package com.example.gatewayservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/gateway")
public class GatewayController {

    private final Map<String, Integer> userCounter = new HashMap<>();

    @GetMapping("/forward")
    public String forwardRequest(@RequestParam String username) {
        userCounter.putIfAbsent(username, 0);
        if  (userCounter.get(username) >= 3) {
            return "Rate limit exceeded for today";
        }
        userCounter.put(username, userCounter.get(username) + 1);
        // forward to claim service ?
        return "Success";

    }
}

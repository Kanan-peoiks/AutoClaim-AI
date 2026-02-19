package com.example.claimservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "pricing-service", url = "http://localhost:8083")
public interface PricingClient {
    @GetMapping("/api/prices/{partName}")
    double getPrice(@PathVariable String partName);
}

package com.example.claimservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "pricing-service", url = "${pricing.service.url}")
public interface PricingClient {

    @GetMapping("/api/prices/{partName}")
    double getPrice(@PathVariable("partName") String partName);
}
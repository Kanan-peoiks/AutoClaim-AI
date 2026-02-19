package com.example.pricingservice.controller;

import com.example.pricingservice.repository.PartPriceRepository;
import com.example.pricingservice.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class PricingController {
    private final PricingService pricingService;

    @GetMapping("/{partName}")
    public double getPrice(@PathVariable String partName) {
        return pricingService.getPrice(partName);
    }



}

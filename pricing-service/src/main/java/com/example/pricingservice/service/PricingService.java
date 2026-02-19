package com.example.pricingservice.service;

import com.example.pricingservice.entity.PartPrice;
import com.example.pricingservice.repository.PartPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PricingService {
    private final PartPriceRepository partPriceRepository;

    public double getPrice(String partName) {

        return partPriceRepository.findByPartName(partName)
                .map(PartPrice::getPrice)
                .orElse(0.0);
    }

}

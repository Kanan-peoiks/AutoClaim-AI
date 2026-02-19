package com.example.pricingservice.repository;


import com.example.pricingservice.entity.PartPrice;
import jakarta.servlet.http.Part;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.OptionalInt;

public interface PartPriceRepository extends JpaRepository<PartPrice, Integer> {
    Optional<PartPrice> findByPartName(String partName);
}

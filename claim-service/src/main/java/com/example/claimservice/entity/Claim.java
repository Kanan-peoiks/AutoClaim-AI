package com.example.claimservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "claims")
@Data
@AllArgsConstructor
@Builder
public class Claim {
    @Id
    private String id;
    private String username;
    private String photoUrl;
    private List<String> damagedParts;
    private double totalCost;
    private String jsonData;
}

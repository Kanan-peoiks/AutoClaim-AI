package com.example.claimservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "claims")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String photoUrl;

    @ElementCollection
    @CollectionTable(name = "claim_damaged_parts", joinColumns = @JoinColumn(name = "claim_id"))
    @Column(name = "part_name")
    private List<String> damagedParts;

    private double totalCost;

    @Column(columnDefinition = "TEXT")
    private String jsonData;

    private LocalDateTime createdAt;
}
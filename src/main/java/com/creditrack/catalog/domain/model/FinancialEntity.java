package com.creditrack.catalog.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "financial_entities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "standard_tea", nullable = false)
    private Double standardTea; // e.g. 0.15 for 15%

    @Column(name = "logo_url")
    private String logoUrl;
}

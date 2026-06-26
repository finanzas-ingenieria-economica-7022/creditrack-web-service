package com.creditrack.simulation.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "simulations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Simulation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "vehicle_price", nullable = false)
    private Double vehiclePrice; // PV

    @Column(name = "initial_payment_percentage", nullable = false)
    private Double initialPaymentPercentage; // pCI

    @Column(name = "final_payment_percentage", nullable = false)
    private Double finalPaymentPercentage; // pCF

    @Column(name = "term_months", nullable = false)
    private Integer termMonths; // N

    @Column(name = "interest_rate", nullable = false)
    private Double interestRate; // Tasa

    @Column(name = "interest_rate_type", nullable = false)
    private String interestRateType; // tpTasa (TNA or TEA)

    @Column(name = "capitalization_type", nullable = false)
    private String capitalizationType; // pc (Diaria, Mensual)

    @Column(name = "payment_frequency", nullable = false)
    private Integer paymentFrequency; // frec (30)

    @Column(name = "days_per_year", nullable = false)
    private Integer daysPerYear; // NDxA (360)

    // Initial Costs (capitalized in loan)
    @Column(name = "notary_cost")
    private Double notaryCost;

    @Column(name = "registration_cost")
    private Double registrationCost;

    @Column(name = "appraisal_cost")
    private Double appraisalCost;

    @Column(name = "study_commission")
    private Double studyCommission;

    @Column(name = "activation_commission")
    private Double activationCommission;

    // Periodic Costs
    @Column(name = "gps_fee")
    private Double gpsFee;

    @Column(name = "portes_fee")
    private Double portesFee;

    @Column(name = "admin_fee")
    private Double adminFee;

    @Column(name = "desgravamen_rate")
    private Double desgravamenRate; // pSegDes

    @Column(name = "risk_insurance_rate")
    private Double riskInsuranceRate; // pSegRie

    @Column(name = "cok_rate")
    private Double cokRate; // Tasa de descuento COK

    // Linked Catalog IDs (Logical DDD links, not JPA DB associations)
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "financial_entity_id")
    private Long financialEntityId;

    // Calculated Indicators
    private Double loanAmount;
    private Double tea;
    private Double tem;
    private Double tir;
    private Double tcea;
    private Double van;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "simulation_grace_periods", joinColumns = @JoinColumn(name = "simulation_id"))
    @Column(name = "grace_type")
    @OrderColumn(name = "month_index")
    private List<String> gracePeriods = new ArrayList<>(); // Month index 0 = Month 1 PG type ("T", "P", "S")
}

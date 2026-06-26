package com.creditrack.simulation.interfaces.rest.dto;

import lombok.Data;

import java.util.List;

@Data
public class SimulationRequest {
    private String name;
    private Double vehiclePrice;
    private Double initialPaymentPercentage;
    private Double finalPaymentPercentage;
    private Integer termMonths;
    private Double interestRate;
    private String interestRateType; // TNA or TEA
    private String capitalizationType; // Diaria, Mensual
    private Integer paymentFrequency; // frec (30)
    private Integer daysPerYear; // NDxA (360)

    // Initial Costs
    private Double notaryCost;
    private Double registrationCost;
    private Double appraisalCost;
    private Double studyCommission;
    private Double activationCommission;

    // Periodic Costs
    private Double gpsFee;
    private Double portesFee;
    private Double adminFee;
    private Double desgravamenRate;
    private Double riskInsuranceRate;
    private Double cokRate;

    // Foreign Keys
    private Long customerId;
    private Long vehicleId;
    private Long financialEntityId;

    // Grace periods (ordered list of size termMonths, values: "T", "P", "S")
    private List<String> gracePeriods;
}

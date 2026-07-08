package com.creditrack.simulation.domain.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class FinancialCalculationResult {
    private BigDecimal monthlyPayment;
    private BigDecimal balloonAmount;
    private BigDecimal tea;
    private BigDecimal tem;
    private BigDecimal cokTeaPercent;
    private BigDecimal cokTemPercent;
    private BigDecimal van;
    private BigDecimal tir;
    private BigDecimal tcea;
    private BigDecimal financedAmount;
    private BigDecimal totalInterest;
    private BigDecimal totalInsurance;
    private BigDecimal totalCommissions;
    private BigDecimal totalCreditCost;
    private BigDecimal totalPayment;
    private List<PaymentRow> schedule;
}

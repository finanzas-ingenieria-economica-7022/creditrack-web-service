package com.creditrack.simulation.domain.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentRow {
    private Integer period;
    private LocalDate date;
    private BigDecimal initialBalance;
    private BigDecimal payment;
    private BigDecimal balloonPayment;
    private BigDecimal interest;
    private BigDecimal amortization;
    private BigDecimal insurance;
    private BigDecimal creditLifeInsurance;
    private BigDecimal vehicleInsurance;
    private BigDecimal commission;
    private BigDecimal totalPayment;
    private BigDecimal finalFlow;
    private BigDecimal baseFlow;
    private BigDecimal finalBalance;
    private String graceType;
}

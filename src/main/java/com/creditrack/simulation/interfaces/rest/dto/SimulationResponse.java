package com.creditrack.simulation.interfaces.rest.dto;

import com.creditrack.simulation.domain.model.PaymentRow;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SimulationResponse {
    private Long id;
    private String code;
    private Long clientId;
    private Long vehicleId;
    private Long financialEntityId;
    private String currency;
    private BigDecimal vehiclePrice;
    private BigDecimal downPaymentPercent;
    private BigDecimal financedAmount;
    private Integer termMonths;
    private LocalDate firstPaymentDate;
    private Integer paymentDay;
    private String graceType;
    private Integer graceMonths;
    private BigDecimal balloonPercent;
    private BigDecimal creditLifeInsuranceMonthlyPercent;
    private BigDecimal vehicleInsuranceAnnualPercent;
    private BigDecimal monthlyPayment;
    private BigDecimal teaPercent;
    private BigDecimal temPercent;
    private BigDecimal cokTeaPercent;
    private BigDecimal cokTemPercent;
    private BigDecimal tirPercent;
    private BigDecimal tceaPercent;
    private BigDecimal van;
    private BigDecimal totalInterest;
    private BigDecimal totalInsurance;
    private BigDecimal totalFees;
    private BigDecimal totalPayment;
    private String status;
    private LocalDateTime createdDate;
    private List<PaymentRow> schedule;
}

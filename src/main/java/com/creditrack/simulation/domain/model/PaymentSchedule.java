package com.creditrack.simulation.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "payment_schedules")
public class PaymentSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "simulation_id", nullable = false)
    private Long simulationId;

    private Integer period;
    private LocalDate date;

    @Column(name = "initial_balance", precision = 19, scale = 2)
    private BigDecimal initialBalance;

    @Column(precision = 19, scale = 2)
    private BigDecimal payment;

    @Column(name = "balloon_payment", precision = 19, scale = 2)
    private BigDecimal balloonPayment;

    @Column(precision = 19, scale = 2)
    private BigDecimal interest;

    @Column(precision = 19, scale = 2)
    private BigDecimal amortization;

    @Column(precision = 19, scale = 2)
    private BigDecimal insurance;

    @Column(name = "credit_life_insurance", precision = 19, scale = 2)
    private BigDecimal creditLifeInsurance;

    @Column(name = "vehicle_insurance", precision = 19, scale = 2)
    private BigDecimal vehicleInsurance;

    @Column(precision = 19, scale = 2)
    private BigDecimal commission;

    @Column(name = "total_payment", precision = 19, scale = 2)
    private BigDecimal totalPayment;

    @Column(name = "final_flow", precision = 19, scale = 2)
    private BigDecimal finalFlow;

    @Column(name = "base_flow", precision = 19, scale = 2)
    private BigDecimal baseFlow;

    @Column(name = "final_balance", precision = 19, scale = 2)
    private BigDecimal finalBalance;

    @Column(name = "grace_type", length = 10)
    private String graceType;
}

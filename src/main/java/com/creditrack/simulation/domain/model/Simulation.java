package com.creditrack.simulation.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "simulations")
public class Simulation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 40)
    private String code;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "financial_entity_id")
    private Long financialEntityId;

    @Column(name = "currency", length = 3)
    private String currency = "PEN";

    @Column(name = "vehicle_price", precision = 19, scale = 2)
    private BigDecimal vehiclePrice;

    @Column(name = "down_payment", precision = 19, scale = 2)
    private BigDecimal downPayment;

    @Column(name = "down_payment_percent", precision = 7, scale = 4)
    private BigDecimal downPaymentPercent;

    @Column(name = "financed_amount", precision = 19, scale = 2)
    private BigDecimal financedAmount;

    @Column(name = "term_months")
    private Integer termMonths;

    @Column(name = "tea_percent", precision = 12, scale = 7)
    private BigDecimal teaPercent;

    @Column(name = "tem_percent", precision = 12, scale = 7)
    private BigDecimal temPercent;

    @Column(name = "cok_tea_percent", precision = 12, scale = 7)
    private BigDecimal cokTeaPercent;

    @Column(name = "cok_tem_percent", precision = 12, scale = 7)
    private BigDecimal cokTemPercent;

    @Column(name = "payment_day")
    private Integer paymentDay = 1;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @Column(name = "first_payment_date")
    private LocalDate firstPaymentDate;

    @Column(name = "grace_type", length = 20)
    private String graceType = "NONE";

    @Column(name = "grace_months")
    private Integer graceMonths = 0;

    @Column(name = "balloon_enabled")
    private Boolean balloonEnabled = false;

    @Column(name = "balloon_percent", precision = 7, scale = 4)
    private BigDecimal balloonPercent = BigDecimal.ZERO;

    @Column(name = "balloon_amount", precision = 19, scale = 2)
    private BigDecimal balloonAmount = BigDecimal.ZERO;

    @Column(name = "credit_life_insurance_monthly_percent", precision = 12, scale = 7)
    private BigDecimal creditLifeInsuranceMonthlyPercent = BigDecimal.ZERO;

    @Column(name = "vehicle_insurance_annual_percent", precision = 12, scale = 7)
    private BigDecimal vehicleInsuranceAnnualPercent = BigDecimal.ZERO;

    @Column(name = "monthly_payment", precision = 19, scale = 2)
    private BigDecimal monthlyPayment = BigDecimal.ZERO;

    @Column(name = "van", precision = 19, scale = 2)
    private BigDecimal van = BigDecimal.ZERO;

    @Column(name = "tir_percent", precision = 12, scale = 7)
    private BigDecimal tirPercent = BigDecimal.ZERO;

    @Column(name = "tcea_percent", precision = 12, scale = 7)
    private BigDecimal tceaPercent = BigDecimal.ZERO;

    @Column(name = "total_interest", precision = 19, scale = 2)
    private BigDecimal totalInterest = BigDecimal.ZERO;

    @Column(name = "total_insurance", precision = 19, scale = 2)
    private BigDecimal totalInsurance = BigDecimal.ZERO;

    @Column(name = "total_fees", precision = 19, scale = 2)
    private BigDecimal totalFees = BigDecimal.ZERO;

    @Column(name = "total_payment", precision = 19, scale = 2)
    private BigDecimal totalPayment = BigDecimal.ZERO;

    @Column(length = 30)
    private String status = "Guardado";

    @Column(name = "archived", nullable = false)
    private Boolean archived = false;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}

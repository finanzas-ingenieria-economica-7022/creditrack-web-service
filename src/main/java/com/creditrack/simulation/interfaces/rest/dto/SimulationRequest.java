package com.creditrack.simulation.interfaces.rest.dto;

import com.creditrack.simulation.domain.model.GraceType;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SimulationRequest {
    @NotNull private Long clientId;
    @NotNull private Long vehicleId;
    private Long financialEntityId;

    @NotNull @Positive private BigDecimal vehiclePrice;
    private String currency = "PEN";

    @DecimalMin("0.0") private BigDecimal teaPercent;
    @NotNull @DecimalMin("0.0") @DecimalMax("100.0") private BigDecimal downPaymentPercent;
    @NotNull @Min(1) private Integer termMonths;
    @NotNull @DecimalMin("0.0") private BigDecimal cokTeaPercent;
    @NotNull private LocalDate firstPaymentDate;
    @Min(1) @Max(28) private Integer paymentDay = 1;
    private GraceType graceType = GraceType.NONE;
    @Min(0) private Integer graceMonths = 0;
    @DecimalMin("0.0") @DecimalMax("99.9999") private BigDecimal balloonPercent = BigDecimal.ZERO;
    private Boolean creditLifeInsuranceEnabled = true;
    @DecimalMin("0.0") private BigDecimal creditLifeInsuranceMonthlyPercent;
    private Boolean vehicleInsuranceEnabled = true;
    @DecimalMin("0.0") private BigDecimal vehicleInsuranceAnnualPercent;

    private Integer minTermMonths;
    private Integer maxTermMonths;
    private BigDecimal minDownPaymentPercent;
}

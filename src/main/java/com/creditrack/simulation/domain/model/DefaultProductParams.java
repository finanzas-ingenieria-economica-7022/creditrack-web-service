package com.creditrack.simulation.domain.model;

import java.math.BigDecimal;

public record DefaultProductParams(
    BigDecimal teaPercent,
    Integer minTermMonths,
    Integer maxTermMonths,
    BigDecimal minDownPaymentPercent,
    BigDecimal maxDownPaymentPercent,
    Boolean balloonAllowed,
    BigDecimal maxBalloonPercent,
    BigDecimal creditLifeInsuranceMonthlyPercent,
    BigDecimal vehicleInsuranceAnnualPercent,
    BigDecimal monthlyFee,
    BigDecimal adminCost,
    BigDecimal notaryCost,
    String currency
) implements ProductParams {
    public BigDecimal getTeaPercent() { return teaPercent; }
    public Integer getMinTermMonths() { return minTermMonths; }
    public Integer getMaxTermMonths() { return maxTermMonths; }
    public BigDecimal getMinDownPaymentPercent() { return minDownPaymentPercent; }
    public BigDecimal getMaxDownPaymentPercent() { return maxDownPaymentPercent; }
    public Boolean getBalloonAllowed() { return balloonAllowed; }
    public BigDecimal getMaxBalloonPercent() { return maxBalloonPercent; }
    public BigDecimal getCreditLifeInsuranceMonthlyPercent() { return creditLifeInsuranceMonthlyPercent; }
    public BigDecimal getVehicleInsuranceAnnualPercent() { return vehicleInsuranceAnnualPercent; }
    public BigDecimal getMonthlyFee() { return monthlyFee; }
    public BigDecimal getAdminCost() { return adminCost; }
    public BigDecimal getNotaryCost() { return notaryCost; }
    public String getCurrency() { return currency; }
}

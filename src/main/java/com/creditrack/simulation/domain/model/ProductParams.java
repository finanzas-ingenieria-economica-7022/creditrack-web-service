package com.creditrack.simulation.domain.model;

import java.math.BigDecimal;

public interface ProductParams {
    BigDecimal getTeaPercent();
    Integer getMinTermMonths();
    Integer getMaxTermMonths();
    BigDecimal getMinDownPaymentPercent();
    BigDecimal getMaxDownPaymentPercent();
    Boolean getBalloonAllowed();
    BigDecimal getMaxBalloonPercent();
    BigDecimal getCreditLifeInsuranceMonthlyPercent();
    BigDecimal getVehicleInsuranceAnnualPercent();
    BigDecimal getMonthlyFee();
    BigDecimal getAdminCost();
    BigDecimal getNotaryCost();
    String getCurrency();
}

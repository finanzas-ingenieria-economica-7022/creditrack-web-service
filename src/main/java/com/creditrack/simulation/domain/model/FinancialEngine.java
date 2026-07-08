package com.creditrack.simulation.domain.model;

import org.springframework.stereotype.Component;

import java.math.*;
import java.time.LocalDate;
import java.util.*;

@Component
public class FinancialEngine {
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWELVE = new BigDecimal("12");
    private static final BigDecimal CENT = new BigDecimal("0.01");

    public record Input(
        BigDecimal vehiclePrice,
        BigDecimal teaPercent,
        BigDecimal downPaymentPercent,
        BigDecimal balloonPercent,
        BigDecimal cokTeaPercent,
        int termMonths,
        GraceType graceType,
        int graceMonths,
        LocalDate firstPaymentDate,
        int paymentDay,
        BigDecimal creditLifeInsuranceMonthlyPercent,
        BigDecimal vehicleInsuranceAnnualPercent,
        ProductParams product
    ) {}

    public FinancialCalculationResult calculate(Input input) {
        validate(input);
        ProductParams product = input.product();
        BigDecimal tea = input.teaPercent() == null ? product.getTeaPercent() : input.teaPercent();
        BigDecimal monthlyRate = effectiveMonthlyRate(tea);
        BigDecimal cokTea = input.cokTeaPercent();
        BigDecimal cokMonthlyRate = effectiveMonthlyRate(cokTea);
        BigDecimal principal = input.vehiclePrice().multiply(
            BigDecimal.ONE.subtract(input.downPaymentPercent().divide(HUNDRED, MC)), MC);
        BigDecimal balloon = input.vehiclePrice().multiply(input.balloonPercent().divide(HUNDRED, MC), MC);
        BigDecimal lifeRate = input.creditLifeInsuranceMonthlyPercent().divide(HUNDRED, MC);
        BigDecimal vehicleInsuranceMonthlyPercent = input.vehicleInsuranceAnnualPercent()
            .divide(TWELVE, 4, RoundingMode.HALF_UP);
        BigDecimal vehicleInsurance = input.vehiclePrice()
            .multiply(vehicleInsuranceMonthlyPercent.divide(HUNDRED, MC), MC);
        BigDecimal creditLifeInsurance = principal.multiply(lifeRate, MC);
        BigDecimal fee = BigDecimal.ZERO;

        BigDecimal balanceAfterGrace = principal;
        for (int period = 1; period <= input.graceMonths(); period++) {
            BigDecimal interest = balanceAfterGrace.multiply(monthlyRate, MC);
            if (input.graceType() == GraceType.TOTAL) {
                balanceAfterGrace = balanceAfterGrace.add(interest).add(creditLifeInsurance).add(vehicleInsurance);
            } else if (input.graceType() == GraceType.PARTIAL) {
                balanceAfterGrace = balanceAfterGrace.add(creditLifeInsurance).add(vehicleInsurance);
            }
        }

        int regularPeriods = input.termMonths() - input.graceMonths();
        BigDecimal balloonPv = monthlyRate.signum() == 0
            ? balloon
            : balloon.divide(BigDecimal.ONE.add(monthlyRate).pow(regularPeriods, MC), MC);
        if (balloonPv.compareTo(balanceAfterGrace) > 0) {
            throw new IllegalArgumentException("La cuota balon es demasiado alta para el monto financiado");
        }
        BigDecimal installment = annuity(balanceAfterGrace.subtract(balloonPv), monthlyRate, regularPeriods);

        List<PaymentRow> schedule = new ArrayList<>();
        List<BigDecimal> cashflows = new ArrayList<>();
        List<BigDecimal> baseCashflows = new ArrayList<>();
        cashflows.add(principal);
        baseCashflows.add(principal);
        BigDecimal balance = principal;
        BigDecimal totalInterestAccrued = BigDecimal.ZERO;
        BigDecimal totalInsuranceAccrued = BigDecimal.ZERO;
        BigDecimal totalPaymentAccrued = BigDecimal.ZERO;

        for (int period = 1; period <= input.termMonths(); period++) {
            BigDecimal initial = balance;
            BigDecimal interest = initial.multiply(monthlyRate, MC);
            totalInterestAccrued = totalInterestAccrued.add(interest);
            BigDecimal life = creditLifeInsurance;
            totalInsuranceAccrued = totalInsuranceAccrued.add(life).add(vehicleInsurance);
            BigDecimal amortization = BigDecimal.ZERO;
            BigDecimal basePayment = BigDecimal.ZERO;
            BigDecimal balloonPayment = BigDecimal.ZERO;
            BigDecimal paidLife = life;
            BigDecimal paidVehicleInsurance = vehicleInsurance;

            if (period <= input.graceMonths() && input.graceType() == GraceType.TOTAL) {
                balance = balance.add(interest).add(life).add(vehicleInsurance);
                paidLife = BigDecimal.ZERO;
                paidVehicleInsurance = BigDecimal.ZERO;
            } else if (period <= input.graceMonths() && input.graceType() == GraceType.PARTIAL) {
                basePayment = interest;
                balance = balance.add(life).add(vehicleInsurance);
                paidLife = BigDecimal.ZERO;
                paidVehicleInsurance = BigDecimal.ZERO;
            } else {
                BigDecimal scheduledAmortization = installment.subtract(interest);
                boolean last = period == input.termMonths();
                if (last) {
                    BigDecimal expected = balance.subtract(scheduledAmortization).subtract(balloon);
                    if (expected.abs().compareTo(CENT) > 0) {
                        throw new IllegalStateException("El cronograma deja un saldo residual de " + money(expected));
                    }
                    amortization = balance;
                    basePayment = installment;
                    balloonPayment = balloon;
                    balance = BigDecimal.ZERO;
                } else {
                    amortization = scheduledAmortization;
                    basePayment = installment;
                    balance = balance.subtract(amortization);
                }
            }

            BigDecimal total = basePayment.add(paidLife).add(paidVehicleInsurance).add(balloonPayment);
            totalPaymentAccrued = totalPaymentAccrued.add(total);
            cashflows.add(total.negate());
            BigDecimal baseFlow = baseCashflow(period, input, interest, installment, balloon).negate();
            baseCashflows.add(baseFlow);
            BigDecimal displayedPayment = basePayment.add(paidLife).add(paidVehicleInsurance).add(balloonPayment);
            BigDecimal finalFlow = displayedPayment.negate();
            schedule.add(row(period,
                dueDate(input.firstPaymentDate(), input.paymentDay(), period),
                initial, displayedPayment, balloonPayment, interest, amortization,
                life.add(vehicleInsurance), life, vehicleInsurance, fee,
                total, finalFlow, baseFlow, balance,
                period <= input.graceMonths() ? input.graceType() : GraceType.NONE));
        }

        if (balance.abs().compareTo(CENT) > 0) {
            throw new IllegalStateException("El saldo final no es cero: " + money(balance));
        }
        BigDecimal monthlyIrr = irr(baseCashflows);
        BigDecimal annualIrr = BigDecimal.ONE.add(monthlyIrr).pow(12, MC).subtract(BigDecimal.ONE);
        BigDecimal totalFees = sum(schedule, PaymentRow::getCommission);

        FinancialCalculationResult result = new FinancialCalculationResult();
        result.setMonthlyPayment(money(installment));
        result.setBalloonAmount(money(balloon));
        result.setTea(ratePercent(tea));
        result.setTem(ratePercent(monthlyRate.multiply(HUNDRED)));
        result.setCokTeaPercent(ratePercent(cokTea));
        result.setCokTemPercent(ratePercent(cokMonthlyRate.multiply(HUNDRED)));
        result.setTir(ratePercent(monthlyIrr.multiply(HUNDRED)));
        result.setTcea(ratePercent(annualIrr.multiply(HUNDRED)));
        result.setVan(money(npv(baseCashflows, cokMonthlyRate)));
        result.setFinancedAmount(money(principal));
        result.setTotalInterest(money(totalInterestAccrued));
        result.setTotalInsurance(money(totalInsuranceAccrued));
        result.setTotalCommissions(money(totalFees));
        result.setTotalCreditCost(money(totalPaymentAccrued.subtract(principal)));
        result.setTotalPayment(money(totalPaymentAccrued));
        result.setSchedule(schedule);
        return result;
    }

    private void validate(Input i) {
        if (i == null) throw new IllegalArgumentException("Los datos de entrada son obligatorios");
        if (i.vehiclePrice() == null || i.vehiclePrice().signum() <= 0)
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        if (i.product() == null) throw new IllegalArgumentException("El producto financiero es obligatorio");
        if (i.downPaymentPercent() == null || i.balloonPercent() == null)
            throw new IllegalArgumentException("Los porcentajes son obligatorios");
        if (i.creditLifeInsuranceMonthlyPercent() == null || i.vehicleInsuranceAnnualPercent() == null)
            throw new IllegalArgumentException("Los seguros son obligatorios");
        if (i.creditLifeInsuranceMonthlyPercent().signum() < 0 || i.vehicleInsuranceAnnualPercent().signum() < 0)
            throw new IllegalArgumentException("Los seguros no pueden ser negativos");
        if (i.teaPercent() != null && i.teaPercent().signum() < 0)
            throw new IllegalArgumentException("La TEA no puede ser negativa");
        if (i.cokTeaPercent() == null) throw new IllegalArgumentException("El COK es obligatorio");
        if (i.cokTeaPercent().signum() < 0) throw new IllegalArgumentException("El COK no puede ser negativo");
        BigDecimal tea = i.teaPercent() == null ? i.product().getTeaPercent() : i.teaPercent();
        if (i.cokTeaPercent().compareTo(tea) < 0)
            throw new IllegalArgumentException("El COK no puede ser menor a la TEA del producto");
        if (i.graceType() == null) throw new IllegalArgumentException("El tipo de gracia es obligatorio");
        if (i.firstPaymentDate() == null)
            throw new IllegalArgumentException("La fecha de primera cuota es obligatoria");
        if (i.termMonths() < i.product().getMinTermMonths() || i.termMonths() > i.product().getMaxTermMonths())
            throw new IllegalArgumentException("El plazo no esta permitido por el producto");
        if (i.graceMonths() < 0 || i.graceMonths() >= i.termMonths())
            throw new IllegalArgumentException("La gracia debe ser menor al plazo total");
        if (i.downPaymentPercent().compareTo(i.product().getMinDownPaymentPercent()) < 0
            || i.downPaymentPercent().compareTo(i.product().getMaxDownPaymentPercent()) > 0)
            throw new IllegalArgumentException("La cuota inicial no esta permitida por el producto");
        if (i.balloonPercent().signum() < 0
            || i.balloonPercent().compareTo(i.product().getMaxBalloonPercent()) > 0
            || (i.balloonPercent().signum() > 0 && !Boolean.TRUE.equals(i.product().getBalloonAllowed())))
            throw new IllegalArgumentException("La cuota balon no esta permitida por el producto");
        if (i.paymentDay() < 1 || i.paymentDay() > 28)
            throw new IllegalArgumentException("El dia de pago debe estar entre 1 y 28");
    }

    private PaymentRow row(int period, LocalDate date, BigDecimal initial, BigDecimal payment,
                           BigDecimal balloon, BigDecimal interest, BigDecimal amortization,
                           BigDecimal insurance, BigDecimal creditLifeIns, BigDecimal vehicleIns,
                           BigDecimal fee, BigDecimal total, BigDecimal finalFlow,
                           BigDecimal baseFlow, BigDecimal end, GraceType grace) {
        PaymentRow r = new PaymentRow();
        r.setPeriod(period); r.setDate(date);
        r.setInitialBalance(money(initial)); r.setPayment(money(payment));
        r.setBalloonPayment(money(balloon)); r.setInterest(money(interest));
        r.setAmortization(money(amortization)); r.setInsurance(money(insurance));
        r.setCommission(money(fee)); r.setCreditLifeInsurance(money(creditLifeIns));
        r.setVehicleInsurance(money(vehicleIns)); r.setFinalFlow(money(finalFlow));
        r.setBaseFlow(money(baseFlow)); r.setTotalPayment(money(total));
        r.setFinalBalance(money(end)); r.setGraceType(grace.name());
        return r;
    }

    private BigDecimal annuity(BigDecimal capital, BigDecimal r, int n) {
        if (r.signum() == 0) return capital.divide(BigDecimal.valueOf(n), MC);
        BigDecimal factor = BigDecimal.ONE.add(r).pow(n, MC);
        return capital.multiply(r.multiply(factor, MC), MC).divide(factor.subtract(BigDecimal.ONE), MC);
    }

    private BigDecimal irr(List<BigDecimal> flows) {
        double low = -0.9999, high = 1.0;
        double lv = npvDouble(flows, low), hv = npvDouble(flows, high);
        while (Math.signum(lv) == Math.signum(hv) && high < 128) {
            high *= 2;
            hv = npvDouble(flows, high);
        }
        if (!Double.isFinite(lv) || !Double.isFinite(hv) || Math.signum(lv) == Math.signum(hv))
            throw new IllegalStateException("No existe una TIR valida para los flujos generados");
        for (int x = 0; x < 250; x++) {
            double mid = (low + high) / 2, value = npvDouble(flows, mid);
            if (Math.abs(value) < 1e-9) return rate(mid);
            if (Math.signum(value) == Math.signum(lv)) { low = mid; lv = value; } else high = mid;
        }
        return rate((low + high) / 2);
    }

    private double npvDouble(List<BigDecimal> flows, double r) {
        double v = 0;
        for (int i = 0; i < flows.size(); i++) v += flows.get(i).doubleValue() / Math.pow(1 + r, i);
        return v;
    }

    private BigDecimal npv(List<BigDecimal> flows, BigDecimal discount) {
        BigDecimal v = BigDecimal.ZERO;
        for (int i = 0; i < flows.size(); i++)
            v = v.add(flows.get(i).divide(BigDecimal.ONE.add(discount).pow(i, MC), MC));
        return v;
    }

    private BigDecimal effectiveMonthlyRate(BigDecimal annualEffectivePercent) {
        return rate(Math.pow(1d + annualEffectivePercent.divide(HUNDRED, MC).doubleValue(), 1d / 12d) - 1d);
    }

    private BigDecimal baseCashflow(int period, Input input, BigDecimal interest,
                                    BigDecimal baseInstallment, BigDecimal balloon) {
        if (period <= input.graceMonths() && input.graceType() == GraceType.TOTAL) return BigDecimal.ZERO;
        if (period <= input.graceMonths() && input.graceType() == GraceType.PARTIAL) return interest;
        return period == input.termMonths() ? baseInstallment.add(balloon) : baseInstallment;
    }

    private BigDecimal sum(List<PaymentRow> rows, java.util.function.Function<PaymentRow, BigDecimal> f) {
        return rows.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDate dueDate(LocalDate first, int day, int period) {
        return first.plusMonths(period - 1L).withDayOfMonth(day);
    }

    private BigDecimal money(BigDecimal v) { return v.setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal rate(double v) { return BigDecimal.valueOf(v).setScale(12, RoundingMode.HALF_UP); }
    private BigDecimal ratePercent(BigDecimal v) { return v.setScale(7, RoundingMode.HALF_UP); }
}

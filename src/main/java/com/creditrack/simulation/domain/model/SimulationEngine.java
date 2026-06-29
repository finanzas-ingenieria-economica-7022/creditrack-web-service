package com.creditrack.simulation.domain.model;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SimulationEngine {

    /**
     * Replicates the exact financial logic from the Excel sheet 'Frances'.
     *
     * @param sim The simulation parameters
     * @return SimulationResult containing the populated simulation entity and schedule
     */
    public SimulationResult calculate(Simulation sim) {
        // --- 1. DATOS Y CONSTANTES INICIALES ---
        double pv = sim.getVehiclePrice(); // PV
        double pci = sim.getInitialPaymentPercentage(); // pCI
        double pcf = sim.getFinalPaymentPercentage(); // pCF
        int n = sim.getTermMonths(); // N
        double interestRate = sim.getInterestRate(); // Tasa
        String tpTasa = sim.getInterestRateType(); // tpTasa (TNA or TEA)
        int frec = sim.getPaymentFrequency() != null ? sim.getPaymentFrequency() : 30; // frec
        int ndxa = sim.getDaysPerYear() != null ? sim.getDaysPerYear() : 360; // NDxA

        double notary = sim.getNotaryCost() != null ? sim.getNotaryCost() : 0.0;
        double registration = sim.getRegistrationCost() != null ? sim.getRegistrationCost() : 0.0;
        double appraisal = sim.getAppraisalCost() != null ? sim.getAppraisalCost() : 0.0;

        double pSegDes = sim.getDesgravamenRate() != null ? sim.getDesgravamenRate() : 0.0; // pSegDes
        double pSegRie = sim.getRiskInsuranceRate() != null ? sim.getRiskInsuranceRate() : 0.0; // pSegRie
        double cok = sim.getCokRate() != null ? sim.getCokRate() : 0.0; // COK

        // --- 2. CALCULOS DE RESULTADOS INICIALES ---
        // Cuota inicial: CI = pCI * PV
        double ci = pv * pci;
        
        // Cuota final: CF = pCF * PV
        double cf = pv * pcf;

        // Monto del préstamo / Monto a Financiar
        double prestamo = (pv - ci) + notary + registration + appraisal;

        // Tasa efectiva anual (TEA)
        double tea;
        if ("TNA".equalsIgnoreCase(tpTasa)) {
            tea = Math.pow(1 + interestRate / 360.0, 360.0) - 1;
        } else {
            tea = interestRate;
        }

        // Tasa efectiva mensual (TEM)
        double tem = Math.pow(1 + tea, (double) frec / ndxa) - 1;

        // % de Seguro desgrav. per.
        double pSegDesPer = pSegDes * frec / 30.0;

        // Seguro riesgo periódico
        double segRiePer = pSegRie * pv * ((double) frec / ndxa);

        // Saldo a financiar con cuotas: Saldo = Prestamo - CF / (1+TEM)^N
        double baseForBalloon = 1 + tem;
        double balloonDiscountFactor = Math.pow(baseForBalloon, n);
        double sicfStart = cf / balloonDiscountFactor; // Saldo Inicial Cuota Final en periodo 1
        double saldoRegularStart = prestamo - sicfStart; // Saldo a financiar en periodo 1

        // --- 3. CONSTRUCCION DEL CRONOGRAMA ---
        List<ScheduleItem> schedule = new ArrayList<>();

        // Periodo 0
        schedule.add(ScheduleItem.builder()
                .month(0)
                .graceType("N")
                .balloonInitialBalance(0.0)
                .balloonInterest(0.0)
                .balloonAmortization(0.0)
                .balloonDesgravamen(0.0)
                .balloonFinalBalance(0.0)
                .regularInitialBalance(0.0)
                .regularInterest(0.0)
                .regularCuota(0.0)
                .regularAmortization(0.0)
                .regularDesgravamen(0.0)
                .regularFinalBalance(prestamo)
                .riskInsurance(0.0)
                .gps(0.0)
                .portes(0.0)
                .adminFee(0.0)
                .netCashFlow(prestamo) // Positivo
                .build());

        double currentSicf = sicfStart;
        double currentSi = saldoRegularStart;

        // Loop de Periodos 1 a N
        for (int t = 1; t <= n; t++) {
            // Grace Period Type (PG)
            String pg = "S"; // Default: Sin Gracia
            if (sim.getGracePeriods() != null && t - 1 < sim.getGracePeriods().size()) {
                pg = sim.getGracePeriods().get(t - 1);
            }

            // A. Cronograma de la Cuota Final (Balón)
            double sicf = currentSicf;
            double icf = -sicf * tem;
            double segDesCF = -sicf * pSegDesPer;
            double acf = 0.0;
            double sfcf = sicf - icf + acf; // compounds interest: sfcf = sicf * (1 + tem)

            if (t == n) {
                // Balloon is fully amortized / paid at final month N
                acf = - (sicf - icf); // Resolves to -CF
                sfcf = 0.0;
            }

            // B. Cronograma de la Cuota Regular
            double si = currentSi;
            double i = -si * tem;
            double segDes = -si * pSegDesPer;

            double cuota;
            double amort;
            double sf;

            if (t == n) {
                // Liquidate regular balance fully at final month N (pure interest + amort)
                amort = -si;
                cuota = amort + i;
                sf = 0.0;
            } else {
                if ("T".equalsIgnoreCase(pg)) {
                    cuota = 0.0;
                    amort = 0.0;
                    sf = si - i; // Capitalize interest
                } else if ("P".equalsIgnoreCase(pg)) {
                    cuota = i; // Interest only (paid monthly)
                    amort = 0.0;
                    sf = si;
                } else {
                    // Sin Gracia: Calcula PMT based on remaining regular periods with pure TEM
                    int remainingRegularPeriods = 0;
                    for (int k = t; k <= n; k++) {
                        String futurePg = "S";
                        if (sim.getGracePeriods() != null && k - 1 < sim.getGracePeriods().size()) {
                            futurePg = sim.getGracePeriods().get(k - 1);
                        }
                        if ("S".equalsIgnoreCase(futurePg)) {
                            remainingRegularPeriods++;
                        }
                    }
                    if (remainingRegularPeriods == 0) remainingRegularPeriods = 1;
                    
                    cuota = pmt(tem, remainingRegularPeriods, si);
                    amort = cuota - i;
                    sf = si + amort;
                }
            }

            // C. Costes de Operación
            double segRieVal = -segRiePer;
            double gpsVal = sim.getGpsFee() != null ? -sim.getGpsFee() : 0.0;
            double portesVal = sim.getPortesFee() != null ? -sim.getPortesFee() : 0.0;
            double adminVal = sim.getAdminFee() != null ? -sim.getAdminFee() : 0.0;

            // D. Flujo Neto de Caja (summing regular cuota, balloon amortization, insurance, and portes/fees)
            double regularCuotaVal = ("T".equalsIgnoreCase(pg) && t < n) ? 0.0 : ("P".equalsIgnoreCase(pg) && t < n) ? i : cuota;
            double netFlow = regularCuotaVal + acf + segDes + segDesCF + segRieVal + gpsVal + portesVal + adminVal;

            schedule.add(ScheduleItem.builder()
                    .month(t)
                    .graceType(pg)
                    .balloonInitialBalance(sicf)
                    .balloonInterest(icf)
                    .balloonAmortization(acf)
                    .balloonDesgravamen(segDesCF)
                    .balloonFinalBalance(sfcf)
                    .regularInitialBalance(si)
                    .regularInterest(i)
                    .regularCuota(regularCuotaVal)
                    .regularAmortization(amort)
                    .regularDesgravamen(segDes)
                    .regularFinalBalance(sf)
                    .riskInsurance(segRieVal)
                    .gps(gpsVal)
                    .portes(portesVal)
                    .adminFee(adminVal)
                    .netCashFlow(netFlow)
                    .build());

            currentSicf = sfcf;
            currentSi = sf;
        }

        // --- 4. INDICADORES DE RENTABILIDAD ---
        double[] cashFlows = new double[schedule.size()];
        for (int iIdx = 0; iIdx < schedule.size(); iIdx++) {
            cashFlows[iIdx] = schedule.get(iIdx).getNetCashFlow();
        }

        double coki = Math.pow(1 + cok, (double) frec / ndxa) - 1;

        double npv = 0.0;
        for (int t = 1; t <= n; t++) {
            npv += cashFlows[t] / Math.pow(1 + coki, t);
        }
        double van = cashFlows[0] + npv; // F0 (prestamo) is positive

        double tir = calculateIRR(cashFlows);
        double tcea = Math.pow(1 + tir, (double) ndxa / frec) - 1;

        // --- 5. COMPLETAR LA ENTIDAD SIMULATION ---
        sim.setLoanAmount(prestamo);
        sim.setTea(tea);
        sim.setTem(tem);
        sim.setTir(tir);
        sim.setTcea(tcea);
        sim.setVan(van);

        return new SimulationResult(sim, schedule);
    }

    /**
     * Excel-style PMT helper.
     */
    private double pmt(double rate, int nper, double pv) {
        if (rate == 0.0) {
            return -pv / nper;
        }
        double temp = Math.pow(1 + rate, nper);
        return -(pv * rate * temp) / (temp - 1);
    }

    /**
     * Newton-Raphson Solver for Internal Rate of Return (IRR / TIR).
     */
    private double calculateIRR(double[] flows) {
        double r = 0.01; // Initial guess: 1%
        double precision = 1e-11;
        int maxIteration = 1000;

        for (int i = 0; i < maxIteration; i++) {
            double f = 0.0;
            double df = 0.0;

            for (int t = 0; t < flows.length; t++) {
                double discount = Math.pow(1 + r, t);
                f += flows[t] / discount;
                if (t > 0) {
                    df -= (t * flows[t]) / (discount * (1 + r));
                }
            }

            if (Math.abs(df) < 1e-12) {
                break;
            }

            double rNext = r - f / df;
            if (Math.abs(rNext - r) < precision) {
                return rNext;
            }
            r = rNext;
        }

        return r; // Fallback to current best estimate
    }
}

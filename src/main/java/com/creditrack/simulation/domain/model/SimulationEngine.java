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
        double pv = sim.getVehiclePrice(); // PV (Excel Cell D4)
        double pci = sim.getInitialPaymentPercentage(); // pCI (Excel Cell D6)
        double pcf = sim.getFinalPaymentPercentage(); // pCF (Excel Cell D7)
        int n = sim.getTermMonths(); // N (Excel Cell J7)
        double interestRate = sim.getInterestRate(); // Tasa (Excel Cell D9)
        String tpTasa = sim.getInterestRateType(); // tpTasa (Excel Cell D10)
        int frec = sim.getPaymentFrequency() != null ? sim.getPaymentFrequency() : 30; // frec (Excel Cell D12)
        int ndxa = sim.getDaysPerYear() != null ? sim.getDaysPerYear() : 360; // NDxA (Excel Cell D13)

        double notary = sim.getNotaryCost() != null ? sim.getNotaryCost() : 0.0;
        double registration = sim.getRegistrationCost() != null ? sim.getRegistrationCost() : 0.0;
        double appraisal = sim.getAppraisalCost() != null ? sim.getAppraisalCost() : 0.0;
        double study = sim.getStudyCommission() != null ? sim.getStudyCommission() : 0.0;
        double activation = sim.getActivationCommission() != null ? sim.getActivationCommission() : 0.0;

        double pSegDes = sim.getDesgravamenRate() != null ? sim.getDesgravamenRate() : 0.0; // pSegDes (Excel Cell D24)
        double pSegRie = sim.getRiskInsuranceRate() != null ? sim.getRiskInsuranceRate() : 0.0; // pSegRie (Excel Cell D25)
        double cok = sim.getCokRate() != null ? sim.getCokRate() : 0.0; // COK (Excel Cell D27)

        // --- 2. CALCULOS DE RESULTADOS INICIALES (Excel Column J) ---
        // Cuota inicial: CI = pCI * PV (Excel Cell J8)
        double ci = pv * pci;
        
        // Cuota final: CF = pCF * PV (Excel Cell J9)
        double cf = pv * pcf;

        // Monto del préstamo: Prestamo = PV - CI + CostesNotariales + CostesRegistrales... (Excel Cell J10)
        double prestamo = pv - ci + notary + registration + appraisal + study + activation;

        // Tasa efectiva anual (TEA): (Excel Cell J4 / ArrayFormula)
        // =IF(tpTasa="TNA",(1+Tasa/360)^360-1,Tasa) [Daily capitalization is assumed always for TNA]
        double tea;
        if ("TNA".equalsIgnoreCase(tpTasa)) {
            tea = Math.pow(1 + interestRate / 360.0, 360.0) - 1;
        } else {
            tea = interestRate;
        }

        // Tasa efectiva mensual (TEM): TEM = (1+TEA)^(frec/NDxA)-1 (Excel Cell J5)
        double tem = Math.pow(1 + tea, (double) frec / ndxa) - 1;

        // % de Seguro desgrav. per.: pSegDesPer = pSegDes * frec / 30 (Excel Cell J13)
        double pSegDesPer = pSegDes * frec / 30.0;

        // Seguro riesgo periódico: SegRiePer = pSegRie * PV / NCxA = pSegRie * PV * frec / NDxA (Excel Cell J14)
        double segRiePer = pSegRie * pv * ((double) frec / ndxa);

        // Saldo a financiar con cuotas: Saldo = Prestamo - CF / (1+TEM+pSegDes)^(N+1) (Excel Cell J11)
        // Note: dividing by (1+TEM+pSegDes) because the final balloon payment includes capitalized desgravamen
        double baseForBalloon = 1 + tem + pSegDes;
        double balloonDiscountFactor = Math.pow(baseForBalloon, n + 1);
        double sicfStart = cf / balloonDiscountFactor; // Saldo Inicial Cuota Final en periodo 1
        double saldoRegularStart = prestamo - sicfStart; // Saldo a financiar en periodo 1

        // --- 3. CONSTRUCCION DEL CRONOGRAMA ---
        List<ScheduleItem> schedule = new ArrayList<>();

        // Periodo 0 (Fila 31 del Excel)
        // Flujo en Periodo 0 = Prestamo (J31 = Prestamo)
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
                .regularFinalBalance(0.0)
                .riskInsurance(0.0)
                .gps(0.0)
                .portes(0.0)
                .adminFee(0.0)
                .netCashFlow(prestamo) // Positivo (ingreso para el deudor)
                .build());

        double currentSicf = sicfStart;
        double currentSi = saldoRegularStart;

        // Loop de Periodos 1 a N
        for (int t = 1; t <= n; t++) {
            // Grace Period Type (PG) (Excel Column B)
            String pg = "S"; // Default: Sin Gracia
            if (sim.getGracePeriods() != null && t - 1 < sim.getGracePeriods().size()) {
                pg = sim.getGracePeriods().get(t - 1);
            }

            // A. Cronograma de la Cuota Final (Excel Columns C a G)
            // Saldo Inicial Cuota final (C): SICF = IF(NC=1, CF/(1+TEM+pSegDes)^(N+1), SFCF_prev)
            double sicf = currentSicf;
            // Interes Cuota final (D): ICF = -SICF * TEM
            double icf = -sicf * tem;
            // Seguro desg. Cuota final (F): SegDesCF = -SICF * pSegDesPer
            double segDesCF = -sicf * pSegDesPer;
            // Amort. Cuota final (E): ACF = 0 (for periods <= N)
            double acf = 0.0;
            // Saldo Final Cuota Final (G): SFCF = SICF - ICF - SegDesCF + ACF
            // (since ICF and SegDesCF are negative, they add up to capital)
            double sfcf = sicf - icf - segDesCF + acf;

            // B. Cronograma de la Cuota Regular (Excel Columns H to Q)
            // Saldo Inicial Cuota (H): SI = IF(NC=1, Saldo, SF_prev)
            double si = currentSi;
            // Interes (I): I = -SI * TEM
            double i = -si * tem;
            // Seguro desg. Cuota (L): SegDes = -SI * pSegDesPer
            double segDes = -si * pSegDesPer;

            // Cuota (J): Cuota = IF(NC<=N, IF(PG="T",0,IF(PG="P",I,PMT(TEM+pSegDesPer,N-NC+1,SI))), 0)
            double cuota;
            if ("T".equalsIgnoreCase(pg)) {
                cuota = 0.0;
            } else if ("P".equalsIgnoreCase(pg)) {
                cuota = i; // Solo intereses (valor negativo)
            } else {
                // Sin Gracia: Calcula PMT
                double rate = tem + pSegDesPer;
                int remainingPeriods = n - t + 1;
                cuota = pmt(rate, remainingPeriods, si); // returns negative
            }

            // Amortizacion (K): A = IF(NC<=N, IF(OR(PG="T",PG="P"),0,Cuota-I-SegDes),0)
            double amort;
            if ("T".equalsIgnoreCase(pg) || "P".equalsIgnoreCase(pg)) {
                amort = 0.0;
            } else {
                amort = cuota - i - segDes; // negative value
            }

            // Saldo Final (Q): SF = IF(PG="T", SI-I, SI+A)
            // (since I and A are negative: SI-I capitalizes interest, SI+A amortizes balance)
            double sf;
            if ("T".equalsIgnoreCase(pg)) {
                sf = si - i;
            } else {
                sf = si + amort;
            }

            // C. Costes de Operación (Excel Columns M a P)
            // Seguro Riesgo (M): -SegRiePer
            double segRieVal = -segRiePer;
            // GPS (N): -GPSPer
            double gpsVal = sim.getGpsFee() != null ? -sim.getGpsFee() : 0.0;
            // Portes (O): -PortesPer
            double portesVal = sim.getPortesFee() != null ? -sim.getPortesFee() : 0.0;
            // Gastos Adm (P): -GasAdmPer
            double adminVal = sim.getAdminFee() != null ? -sim.getAdminFee() : 0.0;

            // D. Flujo (R): =Cuota+SegRie+GPS+Portes+GasAdm+IF(OR(PG="T",PG="P"),SegDes,0)+IF(NC=N+1,ACF,0)
            double desgravamenExtra = ("T".equalsIgnoreCase(pg) || "P".equalsIgnoreCase(pg)) ? segDes : 0.0;
            double netFlow = cuota + segRieVal + gpsVal + portesVal + adminVal + desgravamenExtra;

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
                    .regularCuota(cuota)
                    .regularAmortization(amort)
                    .regularDesgravamen(segDes)
                    .regularFinalBalance(sf)
                    .riskInsurance(segRieVal)
                    .gps(gpsVal)
                    .portes(portesVal)
                    .adminFee(adminVal)
                    .netCashFlow(netFlow)
                    .build());

            // Update parameters for next iteration
            currentSicf = sfcf;
            currentSi = sf;
        }

        // --- 4. PERIODO FINAL N+1 (Fila 68 del Excel: mes 37) ---
        // Month N+1 (t = N + 1)
        int finalMonth = n + 1;
        double sicfFinal = currentSicf;
        double icfFinal = -sicfFinal * tem;
        double segDesCFFinal = -sicfFinal * pSegDesPer;
        // ACF = -SICF + ICF + SegDesCF (Excel Cell E68)
        double acfFinal = -sicfFinal + icfFinal + segDesCFFinal; // Resolves to -CF
        double sfcfFinal = sicfFinal - icfFinal - segDesCFFinal + acfFinal; // Resolves to 0.0

        double segRieValFinal = -segRiePer;
        double gpsValFinal = sim.getGpsFee() != null ? -sim.getGpsFee() : 0.0;
        double portesValFinal = sim.getPortesFee() != null ? -sim.getPortesFee() : 0.0;
        double adminValFinal = sim.getAdminFee() != null ? -sim.getAdminFee() : 0.0;

        // Flujo = SegRie + GPS + Portes + GasAdm + ACF (Cuota = 0, SegDes = 0)
        double netFlowFinal = segRieValFinal + gpsValFinal + portesValFinal + adminValFinal + acfFinal;

        schedule.add(ScheduleItem.builder()
                .month(finalMonth)
                .graceType("S") // treated as regular final payment
                .balloonInitialBalance(sicfFinal)
                .balloonInterest(icfFinal)
                .balloonAmortization(acfFinal)
                .balloonDesgravamen(segDesCFFinal)
                .balloonFinalBalance(sfcfFinal)
                .regularInitialBalance(0.0)
                .regularInterest(0.0)
                .regularCuota(0.0)
                .regularAmortization(0.0)
                .regularDesgravamen(0.0)
                .regularFinalBalance(0.0)
                .riskInsurance(segRieValFinal)
                .gps(gpsValFinal)
                .portes(portesValFinal)
                .adminFee(adminValFinal)
                .netCashFlow(netFlowFinal)
                .build());

        // --- 5. INDICADORES DE RENTABILIDAD ---
        // Extraer los flujos netos para VAN, TIR, TCEA
        double[] cashFlows = new double[schedule.size()];
        for (int iIdx = 0; iIdx < schedule.size(); iIdx++) {
            cashFlows[iIdx] = schedule.get(iIdx).getNetCashFlow();
        }

        // COKi = (1+COK)^(frec/NDxA)-1 (Excel Cell J24)
        double coki = Math.pow(1 + cok, (double) frec / ndxa) - 1;

        // VAN = Prestamo + NPV(COKi, Flujo_1..N+1) (Excel Cell J27)
        double npv = 0.0;
        for (int t = 1; t <= n + 1; t++) {
            npv += cashFlows[t] / Math.pow(1 + coki, t);
        }
        double van = cashFlows[0] + npv; // F0 is positive loanAmount

        // TIR de la operación: (Excel Cell J25)
        double tir = calculateIRR(cashFlows);

        // TCEA: = (1+TIR)^(NDxA/frec) - 1 (Excel Cell J26)
        double tcea = Math.pow(1 + tir, (double) ndxa / frec) - 1;

        // --- 6. COMPLETAR LA ENTIDAD SIMULATION ---
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

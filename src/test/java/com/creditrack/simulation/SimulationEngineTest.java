package com.creditrack.simulation;

import com.creditrack.simulation.domain.model.ScheduleItem;
import com.creditrack.simulation.domain.model.Simulation;
import com.creditrack.simulation.domain.model.SimulationEngine;
import com.creditrack.simulation.domain.model.SimulationResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationEngineTest {

    private final SimulationEngine engine = new SimulationEngine();

    @Test
    public void testReplicateExcelFrenchModel() {
        // --- 1. SET UP EXCEL INPUT VALUES ---
        Simulation sim = new Simulation();
        sim.setName("Simulacion Test Compra Inteligente");
        sim.setVehiclePrice(16000.0); // PV = 16000
        sim.setInitialPaymentPercentage(0.2); // pCI = 20%
        sim.setFinalPaymentPercentage(0.4); // pCF = 40%
        sim.setTermMonths(36); // N = 36
        sim.setInterestRate(0.15); // Tasa = 15%
        sim.setInterestRateType("TNA"); // tpTasa = TNA
        sim.setCapitalizationType("Diaria"); // pc = Diaria
        sim.setPaymentFrequency(30); // frec = 30
        sim.setDaysPerYear(360); // NDxA = 360

        // Initial capitalized costs
        sim.setNotaryCost(100.0);
        sim.setRegistrationCost(75.0);
        sim.setAppraisalCost(0.0);
        sim.setStudyCommission(0.0);
        sim.setActivationCommission(0.0);

        // Periodical costs
        sim.setGpsFee(20.0);
        sim.setPortesFee(3.5);
        sim.setAdminFee(3.5);
        sim.setDesgravamenRate(0.00049); // pSegDes = 0.049%
        sim.setRiskInsuranceRate(0.003); // pSegRie = 0.3%
        sim.setCokRate(0.50); // COK = 50%

        // Grace periods configuration: 3 Total, 3 Partial, 30 Sin Gracia (Total = 36 months)
        List<String> gracePeriods = new ArrayList<>();
        for (int i = 0; i < 3; i++) gracePeriods.add("T"); // Months 1, 2, 3: Total
        for (int i = 0; i < 3; i++) gracePeriods.add("P"); // Months 4, 5, 6: Partial
        for (int i = 0; i < 30; i++) gracePeriods.add("S"); // Months 7 to 36: Sin Gracia
        sim.setGracePeriods(gracePeriods);

        // --- 2. EXECUTE FINANCIAL ENGINE ---
        SimulationResult result = engine.calculate(sim);
        Simulation calculatedSim = result.getSimulation();
        List<ScheduleItem> schedule = result.getSchedule();

        // --- 3. AUDIT GENERAL SIMULATION RESULTS ---
        // Monto Prestamo = 16000 - 3200 + 175 = 12975.0
        assertEquals(12975.0, calculatedSim.getLoanAmount(), 1e-9);

        // TEA = (1 + 0.15/360)^360 - 1 = 16.1797946%
        assertEquals(0.1617979460574055, calculatedSim.getTea(), 1e-9);

        // TEM = (1 + TEA)^(30/360) - 1 = 1.2575815%
        assertEquals(0.012575815353265574, calculatedSim.getTem(), 1e-9);

        // TIR (Monthly IRR) = 1.5861748%
        assertEquals(0.01586174852778721, calculatedSim.getTir(), 1e-7);

        // TCEA = (1 + TIR)^12 - 1 = 20.785636%
        assertEquals(0.207856362664806, calculatedSim.getTcea(), 1e-7);

        // VAN = 4436.18316
        assertEquals(4436.183165604902, calculatedSim.getVan(), 1e-2);

        // --- 4. AUDIT SCHEDULE DETAIL ROWS ---
        // Schedule size must be N + 2 = 38 (includes Month 0 and Month 37)
        assertEquals(38, schedule.size());

        // Month 0
        ScheduleItem m0 = schedule.get(0);
        assertEquals(0, m0.getMonth());
        assertEquals(12975.0, m0.getNetCashFlow(), 1e-9);

        // Month 1 (Grace Type: Total "T")
        ScheduleItem m1 = schedule.get(1);
        assertEquals(1, m1.getMonth());
        assertEquals("T", m1.getGraceType());
        // Balloon initial balance
        assertEquals(3959.009297010821, m1.getBalloonInitialBalance(), 1e-6);
        // Regular initial balance
        assertEquals(9015.99070298918, m1.getRegularInitialBalance(), 1e-6);
        // Regular payment elements
        assertEquals(0.0, m1.getRegularCuota(), 1e-9);
        assertEquals(-4.417835444464698, m1.getRegularDesgravamen(), 1e-6);
        // Periodic costs
        assertEquals(-4.0, m1.getRiskInsurance(), 1e-9);
        assertEquals(-20.0, m1.getGps(), 1e-9);
        assertEquals(-3.5, m1.getPortes(), 1e-9);
        assertEquals(-3.5, m1.getAdminFee(), 1e-9);
        // Month 1 Net Cash Flow: Cuota(0) + RiskInsurance(-4) + GPS(-20) + Portes(-3.5) + Admin(-3.5) + SegDes(-4.4178) = -35.417835
        assertEquals(-35.4178354444647, m1.getNetCashFlow(), 1e-6);

        // Month 4 (Grace Type: Partial "P")
        ScheduleItem m4 = schedule.get(4);
        assertEquals(4, m4.getMonth());
        assertEquals("P", m4.getGraceType());
        // Regular payment elements (should only pay interests)
        assertEquals(-117.71512237083311, m4.getRegularInterest(), 1e-6);
        assertEquals(-117.71512237083311, m4.getRegularCuota(), 1e-6);
        assertEquals(0.0, m4.getRegularAmortization(), 1e-9);
        assertEquals(-4.586613936465782, m4.getRegularDesgravamen(), 1e-6);
        // Net Cash Flow: -117.715 + (-4) + (-20) + (-3.5) + (-3.5) + (-4.586) = -153.301736
        assertEquals(-153.3017363072989, m4.getNetCashFlow(), 1e-6);

        // Month 7 (Grace Type: Sin Gracia "S" - regular payment begins)
        ScheduleItem m7 = schedule.get(7);
        assertEquals(7, m7.getMonth());
        assertEquals("S", m7.getGraceType());
        // Regular payment elements (PMT begins)
        assertEquals(-379.15843387799924, m7.getRegularCuota(), 1e-6);
        // Net Cash Flow: -379.158 + (-4) + (-20) + (-3.5) + (-3.5) = -410.158433
        // Note: SegDes is not added separately here since it is already included in the PMT rate!
        assertEquals(-410.15843387799924, m7.getNetCashFlow(), 1e-6);

        // Month 37 (Final Month N+1: Balloon payment CF is paid)
        ScheduleItem m37 = schedule.get(37);
        assertEquals(37, m37.getMonth());
        // Balloon amortization (CF)
        assertEquals(-6399.999999999974, m37.getBalloonAmortization(), 1e-6);
        // Net Cash Flow: SegRie(-4) + GPS(-20) + Portes(-3.5) + Admin(-3.5) + BalloonAmort(-6399.99) = -6430.99
        assertEquals(-6430.999999999974, m37.getNetCashFlow(), 1e-6);
        // Balloon final balance must be exactly 0
        assertEquals(0.0, m37.getBalloonFinalBalance(), 1e-9);
    }
}

package com.creditrack.simulation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleItem {
    private Integer month; // NC (0 to N+1)
    private String graceType; // PG ("T", "P", "S" or "N" for final)
    
    // Balloon payment cronograma (Cuota final)
    private Double balloonInitialBalance; // SICF
    private Double balloonInterest; // ICF
    private Double balloonAmortization; // ACF
    private Double balloonDesgravamen; // SegDesCF
    private Double balloonFinalBalance; // SFCF

    // Regular payment cronograma (Cuota regular)
    private Double regularInitialBalance; // SI
    private Double regularInterest; // I
    private Double regularCuota; // Cuota (inc Seg Des)
    private Double regularAmortization; // A
    private Double regularDesgravamen; // SegDes
    private Double regularFinalBalance; // SF

    // Periodic costs
    private Double riskInsurance; // SegRie
    private Double gps; // GPS
    private Double portes; // Portes
    private Double adminFee; // GasAdm

    // Total cash flow
    private Double netCashFlow; // Flujo
}

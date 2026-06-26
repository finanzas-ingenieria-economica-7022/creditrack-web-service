package com.creditrack.analytics.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "simulation_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationMetric {

    @Id
    private Long id; // Matches simulationId

    @Column(name = "loan_amount", nullable = false)
    private Double loanAmount;

    @Column(name = "tcea", nullable = false)
    private Double tcea;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "financial_entity_id")
    private Long financialEntityId;
}

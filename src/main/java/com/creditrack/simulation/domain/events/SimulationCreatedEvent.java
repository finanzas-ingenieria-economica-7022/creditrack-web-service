package com.creditrack.simulation.domain.events;

import lombok.Getter;

@Getter
public class SimulationCreatedEvent {
    private final Long simulationId;
    private final Double loanAmount;
    private final Double tcea;
    private final Long vehicleId;
    private final Long financialEntityId;

    public SimulationCreatedEvent(Long simulationId, Double loanAmount, Double tcea, Long vehicleId, Long financialEntityId) {
        this.simulationId = simulationId;
        this.loanAmount = loanAmount;
        this.tcea = tcea;
        this.vehicleId = vehicleId;
        this.financialEntityId = financialEntityId;
    }
}

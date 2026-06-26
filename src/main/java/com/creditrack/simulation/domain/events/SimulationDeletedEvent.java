package com.creditrack.simulation.domain.events;

import lombok.Getter;

@Getter
public class SimulationDeletedEvent {
    private final Long simulationId;

    public SimulationDeletedEvent(Long simulationId) {
        this.simulationId = simulationId;
    }
}

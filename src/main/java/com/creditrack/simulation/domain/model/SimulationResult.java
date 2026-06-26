package com.creditrack.simulation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResult {
    private Simulation simulation;
    private List<ScheduleItem> schedule;
}

package com.creditrack.simulation.interfaces.rest.dto;

import com.creditrack.simulation.domain.model.ScheduleItem;
import com.creditrack.simulation.domain.model.Simulation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResponse {
    private Simulation simulation;
    private List<ScheduleItem> schedule;
}

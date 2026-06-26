package com.creditrack.simulation.interfaces.rest;

import com.creditrack.simulation.domain.events.SimulationCreatedEvent;
import com.creditrack.simulation.domain.events.SimulationDeletedEvent;
import com.creditrack.simulation.domain.model.Simulation;
import com.creditrack.simulation.domain.model.SimulationEngine;
import com.creditrack.simulation.domain.model.SimulationResult;
import com.creditrack.simulation.domain.repositories.SimulationRepository;
import com.creditrack.simulation.interfaces.rest.dto.SimulationRequest;
import com.creditrack.simulation.interfaces.rest.dto.SimulationResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/simulations")
@Tag(name = "6. Simulation Engine", description = "Run parallel credit simulations and retrieve amortization schedules")
public class SimulationController {

    private final SimulationRepository repository;
    private final SimulationEngine engine;
    private final ApplicationEventPublisher eventPublisher;

    public SimulationController(SimulationRepository repository, SimulationEngine engine, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.engine = engine;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping
    public ResponseEntity<SimulationResponse> createSimulation(@RequestBody SimulationRequest request) {
        // Map request to entity
        Simulation sim = new Simulation();
        sim.setName(request.getName());
        sim.setVehiclePrice(request.getVehiclePrice());
        sim.setInitialPaymentPercentage(request.getInitialPaymentPercentage());
        sim.setFinalPaymentPercentage(request.getFinalPaymentPercentage());
        sim.setTermMonths(request.getTermMonths());
        sim.setInterestRate(request.getInterestRate());
        sim.setInterestRateType(request.getInterestRateType());
        sim.setCapitalizationType(request.getCapitalizationType() != null ? request.getCapitalizationType() : "Diaria");
        sim.setPaymentFrequency(request.getPaymentFrequency() != null ? request.getPaymentFrequency() : 30);
        sim.setDaysPerYear(request.getDaysPerYear() != null ? request.getDaysPerYear() : 360);

        sim.setNotaryCost(request.getNotaryCost() != null ? request.getNotaryCost() : 0.0);
        sim.setRegistrationCost(request.getRegistrationCost() != null ? request.getRegistrationCost() : 0.0);
        sim.setAppraisalCost(request.getAppraisalCost() != null ? request.getAppraisalCost() : 0.0);
        sim.setStudyCommission(request.getStudyCommission() != null ? request.getStudyCommission() : 0.0);
        sim.setActivationCommission(request.getActivationCommission() != null ? request.getActivationCommission() : 0.0);

        sim.setGpsFee(request.getGpsFee() != null ? request.getGpsFee() : 0.0);
        sim.setPortesFee(request.getPortesFee() != null ? request.getPortesFee() : 0.0);
        sim.setAdminFee(request.getAdminFee() != null ? request.getAdminFee() : 0.0);
        sim.setDesgravamenRate(request.getDesgravamenRate() != null ? request.getDesgravamenRate() : 0.0);
        sim.setRiskInsuranceRate(request.getRiskInsuranceRate() != null ? request.getRiskInsuranceRate() : 0.0);
        sim.setCokRate(request.getCokRate() != null ? request.getCokRate() : 0.0);

        sim.setCustomerId(request.getCustomerId());
        sim.setVehicleId(request.getVehicleId());
        sim.setFinancialEntityId(request.getFinancialEntityId());
        sim.setCreatedDate(LocalDateTime.now());

        if (request.getGracePeriods() != null) {
            sim.setGracePeriods(request.getGracePeriods());
        }

        // Calculate schedule and metrics
        SimulationResult result = engine.calculate(sim);
        
        // Save parameters and calculated indicators to database
        Simulation savedSimulation = repository.save(result.getSimulation());
        
        // Publish DDD domain event
        eventPublisher.publishEvent(new SimulationCreatedEvent(
                savedSimulation.getId(),
                savedSimulation.getLoanAmount(),
                savedSimulation.getTcea(),
                savedSimulation.getVehicleId(),
                savedSimulation.getFinancialEntityId()
        ));

        // Return parameters + full schedule
        return ResponseEntity.ok(new SimulationResponse(savedSimulation, result.getSchedule()));
    }

    @GetMapping
    public List<Simulation> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SimulationResponse> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(sim -> {
                    // Recalculate schedule dynamically from saved parameters
                    SimulationResult result = engine.calculate(sim);
                    return ResponseEntity.ok(new SimulationResponse(sim, result.getSchedule()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return repository.findById(id)
                .map(sim -> {
                    repository.delete(sim);
                    eventPublisher.publishEvent(new SimulationDeletedEvent(id));
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

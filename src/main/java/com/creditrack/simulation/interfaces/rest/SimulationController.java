package com.creditrack.simulation.interfaces.rest;

import com.creditrack.iam.domain.model.User;
import com.creditrack.iam.domain.repositories.UserRepository;
import com.creditrack.simulation.application.SimulationService;
import com.creditrack.simulation.domain.events.SimulationCreatedEvent;
import com.creditrack.simulation.domain.events.SimulationDeletedEvent;
import com.creditrack.simulation.interfaces.rest.dto.SimulationRequest;
import com.creditrack.simulation.interfaces.rest.dto.SimulationResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulations")
@RequiredArgsConstructor
@Tag(name = "Simulacion de Credito Vehicular", description = "Motor de calculo de planes de pago con amortizacion francesa")
public class SimulationController {

    private final SimulationService simulationService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @PostMapping
    public ResponseEntity<SimulationResponse> create(@Valid @RequestBody SimulationRequest request,
                                                     Authentication auth) {
        Long userId = resolveUserId(auth);
        SimulationResponse response = simulationService.save(request, userId);
        eventPublisher.publishEvent(new SimulationCreatedEvent(
            response.getId(),
            response.getFinancedAmount() != null ? response.getFinancedAmount().doubleValue() : 0.0,
            response.getTceaPercent() != null ? response.getTceaPercent().doubleValue() : 0.0,
            response.getVehicleId(),
            response.getFinancialEntityId()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SimulationResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody SimulationRequest request,
                                                     Authentication auth) {
        Long userId = resolveUserId(auth);
        SimulationResponse response = simulationService.update(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<SimulationResponse>> getAll(Pageable pageable, Authentication auth) {
        Long userId = resolveUserId(auth);
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")
                || a.getAuthority().equalsIgnoreCase("ADMIN"));
        return ResponseEntity.ok(simulationService.findAll(pageable, userId, isAdmin));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SimulationResponse> getById(@PathVariable Long id, Authentication auth) {
        Long userId = resolveUserId(auth);
        return ResponseEntity.ok(simulationService.findById(id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable Long id, Authentication auth) {
        Long userId = resolveUserId(auth);
        simulationService.archive(id, userId);
        eventPublisher.publishEvent(new SimulationDeletedEvent(id));
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(Authentication auth) {
        String username = auth.getName();
        return userRepository.findByUsername(username)
            .map(User::getId)
            .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
    }
}
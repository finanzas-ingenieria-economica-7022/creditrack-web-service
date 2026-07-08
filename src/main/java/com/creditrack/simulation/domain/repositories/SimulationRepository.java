package com.creditrack.simulation.domain.repositories;

import com.creditrack.simulation.domain.model.Simulation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SimulationRepository extends JpaRepository<Simulation, Long> {
    Page<Simulation> findByArchivedFalse(Pageable pageable);
    Page<Simulation> findByOwnerUserIdAndArchivedFalse(Long ownerUserId, Pageable pageable);
    Optional<Simulation> findByIdAndArchivedFalse(Long id);
    Optional<Simulation> findByIdAndOwnerUserIdAndArchivedFalse(Long id, Long ownerUserId);
}

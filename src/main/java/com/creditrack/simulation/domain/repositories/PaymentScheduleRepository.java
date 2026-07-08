package com.creditrack.simulation.domain.repositories;

import com.creditrack.simulation.domain.model.PaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, Long> {
    List<PaymentSchedule> findBySimulationIdOrderByPeriod(Long simulationId);
    void deleteBySimulationId(Long simulationId);
}

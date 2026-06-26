package com.creditrack.analytics.domain.repositories;

import com.creditrack.analytics.domain.model.SimulationMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SimulationMetricRepository extends JpaRepository<SimulationMetric, Long> {
}

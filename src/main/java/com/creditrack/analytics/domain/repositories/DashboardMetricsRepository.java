package com.creditrack.analytics.domain.repositories;

import com.creditrack.analytics.domain.model.DashboardMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DashboardMetricsRepository extends JpaRepository<DashboardMetrics, Long> {
}

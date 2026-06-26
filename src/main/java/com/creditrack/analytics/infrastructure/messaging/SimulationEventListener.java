package com.creditrack.analytics.infrastructure.messaging;

import com.creditrack.analytics.domain.model.DashboardMetrics;
import com.creditrack.analytics.domain.model.SimulationMetric;
import com.creditrack.analytics.domain.repositories.DashboardMetricsRepository;
import com.creditrack.analytics.domain.repositories.SimulationMetricRepository;
import com.creditrack.simulation.domain.events.SimulationCreatedEvent;
import com.creditrack.simulation.domain.events.SimulationDeletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class SimulationEventListener {

    private final SimulationMetricRepository metricRepository;
    private final DashboardMetricsRepository dashboardRepository;

    public SimulationEventListener(SimulationMetricRepository metricRepository, DashboardMetricsRepository dashboardRepository) {
        this.metricRepository = metricRepository;
        this.dashboardRepository = dashboardRepository;
    }

    @EventListener
    @Transactional("analyticsTransactionManager")
    public void handleSimulationCreated(SimulationCreatedEvent event) {
        SimulationMetric metric = new SimulationMetric(
                event.getSimulationId(),
                event.getLoanAmount(),
                event.getTcea(),
                event.getVehicleId(),
                event.getFinancialEntityId()
        );
        metricRepository.save(metric);
        recalculateMetrics();
    }

    @EventListener
    @Transactional("analyticsTransactionManager")
    public void handleSimulationDeleted(SimulationDeletedEvent event) {
        if (metricRepository.existsById(event.getSimulationId())) {
            metricRepository.deleteById(event.getSimulationId());
        }
        recalculateMetrics();
    }

    private void recalculateMetrics() {
        List<SimulationMetric> metrics = metricRepository.findAll();
        if (metrics.isEmpty()) {
            DashboardMetrics dashboard = new DashboardMetrics(1L, 0L, 0.0, 0.0, 0.0, 0.0);
            dashboardRepository.save(dashboard);
            return;
        }

        long count = metrics.size();
        double totalLoan = 0.0;
        double totalTcea = 0.0;
        double maxTcea = -Double.MAX_VALUE;
        double minTcea = Double.MAX_VALUE;

        for (SimulationMetric m : metrics) {
            totalLoan += m.getLoanAmount();
            totalTcea += m.getTcea();
            if (m.getTcea() > maxTcea) maxTcea = m.getTcea();
            if (m.getTcea() < minTcea) minTcea = m.getTcea();
        }

        DashboardMetrics dashboard = new DashboardMetrics(
                1L,
                count,
                totalLoan / count,
                totalTcea / count,
                maxTcea,
                minTcea
        );
        dashboardRepository.save(dashboard);
    }
}

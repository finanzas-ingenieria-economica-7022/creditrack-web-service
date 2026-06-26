package com.creditrack.analytics.interfaces.rest;

import com.creditrack.analytics.domain.model.DashboardMetrics;
import com.creditrack.analytics.domain.repositories.DashboardMetricsRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "7. Analytics Dashboard", description = "Retrieve aggregate statistics of credit simulations")
public class AnalyticsController {

    private final DashboardMetricsRepository repository;

    public AnalyticsController(DashboardMetricsRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardMetrics> getDashboardMetrics() {
        DashboardMetrics metrics = repository.findById(1L)
                .orElse(new DashboardMetrics(1L, 0L, 0.0, 0.0, 0.0, 0.0));
        return ResponseEntity.ok(metrics);
    }
}

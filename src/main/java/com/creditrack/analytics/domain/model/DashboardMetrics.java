package com.creditrack.analytics.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dashboard_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetrics {

    @Id
    private Long id; // Always 1

    @Column(name = "total_simulations")
    private Long totalSimulations;

    @Column(name = "average_loan_amount")
    private Double averageLoanAmount;

    @Column(name = "average_tcea")
    private Double averageTcea;

    @Column(name = "max_tcea")
    private Double maxTcea;

    @Column(name = "min_tcea")
    private Double minTcea;
}

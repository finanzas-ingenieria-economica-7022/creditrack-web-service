package com.creditrack.catalog.domain.repositories;

import com.creditrack.catalog.domain.model.FinancialEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialEntityRepository extends JpaRepository<FinancialEntity, Long> {
}

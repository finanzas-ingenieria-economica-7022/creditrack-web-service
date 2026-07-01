package com.creditrack.catalog.infrastructure.config;

import com.creditrack.catalog.domain.model.FinancialEntity;
import com.creditrack.catalog.domain.repositories.FinancialEntityRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CatalogDataInitializer implements CommandLineRunner {

    private final FinancialEntityRepository repository;

    public CatalogDataInitializer(FinancialEntityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional("catalogTransactionManager")
    public void run(String... args) throws Exception {
        boolean hasInterbank = repository.findAll().stream().anyMatch(e -> "Interbank".equalsIgnoreCase(e.getName()));
        boolean hasBcp = repository.findAll().stream().anyMatch(e -> "BCP".equalsIgnoreCase(e.getName()));

        if (!hasInterbank) {
            repository.save(new FinancialEntity(null, "Interbank", 0.140, "interbank"));
        }
        if (!hasBcp) {
            repository.save(new FinancialEntity(null, "BCP", 0.125, "bcp"));
        }
    }
}

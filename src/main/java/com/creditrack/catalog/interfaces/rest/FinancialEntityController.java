package com.creditrack.catalog.interfaces.rest;

import com.creditrack.catalog.domain.model.FinancialEntity;
import com.creditrack.catalog.domain.repositories.FinancialEntityRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/financial-entities")
@Tag(name = "5. Financial Institutions", description = "CRUD operations for banks and default interest rates")
public class FinancialEntityController {

    private final FinancialEntityRepository repository;

    public FinancialEntityController(FinancialEntityRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<FinancialEntity> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FinancialEntity> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public FinancialEntity create(@RequestBody FinancialEntity entity) {
        return repository.save(entity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FinancialEntity> update(@PathVariable Long id, @RequestBody FinancialEntity details) {
        return repository.findById(id)
                .map(entity -> {
                    entity.setName(details.getName());
                    entity.setStandardTea(details.getStandardTea());
                    entity.setLogoUrl(details.getLogoUrl());
                    return ResponseEntity.ok(repository.save(entity));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return repository.findById(id)
                .map(entity -> {
                    repository.delete(entity);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

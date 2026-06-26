package com.creditrack.catalog.interfaces.rest;

import com.creditrack.catalog.domain.model.Customer;
import com.creditrack.catalog.domain.repositories.CustomerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository repository;

    public CustomerController(CustomerRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Customer> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Customer create(@RequestBody Customer entity) {
        return repository.save(entity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> update(@PathVariable Long id, @RequestBody Customer details) {
        return repository.findById(id)
                .map(entity -> {
                    entity.setFirstName(details.getFirstName());
                    entity.setLastName(details.getLastName());
                    entity.setEmail(details.getEmail());
                    entity.setPhoneNumber(details.getPhoneNumber());
                    entity.setDocumentType(details.getDocumentType());
                    entity.setDocumentNumber(details.getDocumentNumber());
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

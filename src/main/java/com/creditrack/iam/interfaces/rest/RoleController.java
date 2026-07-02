package com.creditrack.iam.interfaces.rest;

import com.creditrack.iam.application.service.RoleService;
import com.creditrack.iam.interfaces.rest.dto.RoleResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "4. Roles", description = "Role and permission catalog")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('role:read')")
    public ResponseEntity<List<RoleResponse>> findAllRoles() {
        return ResponseEntity.ok(roleService.findAllRoles());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('role:read')")
    public ResponseEntity<List<String>> findAllPermissions() {
        return ResponseEntity.ok(roleService.findAllPermissions());
    }
}

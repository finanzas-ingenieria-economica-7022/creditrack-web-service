package com.creditrack.iam.application.service;

import com.creditrack.iam.domain.model.Permission;
import com.creditrack.iam.domain.model.Role;
import com.creditrack.iam.interfaces.rest.dto.RoleResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RoleService {

    public List<RoleResponse> findAllRoles() {
        return Arrays.stream(Role.values())
                .map(role -> RoleResponse.builder()
                        .role(role.name())
                        .permissions(role.getPermissions().stream()
                                .map(Permission::getAuthority)
                                .toList())
                        .build())
                .toList();
    }

    public List<String> findAllPermissions() {
        return Arrays.stream(Permission.values())
                .map(Permission::getAuthority)
                .toList();
    }
}

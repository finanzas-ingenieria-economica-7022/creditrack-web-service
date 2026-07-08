package com.creditrack.iam.domain.model;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public enum Role {
    ROLE_ADMIN(EnumSet.allOf(Permission.class)),
    ROLE_MANAGER(EnumSet.of(
            Permission.USER_READ,
            Permission.USER_UPDATE,
            Permission.PROFILE_READ,
            Permission.PROFILE_UPDATE,
            Permission.PASSWORD_CHANGE,
            Permission.TOKEN_REFRESH,
            Permission.TOKEN_LOGOUT,
            Permission.ROLE_READ
    )),
    ROLE_USER(EnumSet.of(
            Permission.PROFILE_READ,
            Permission.PROFILE_UPDATE,
            Permission.PASSWORD_CHANGE,
            Permission.TOKEN_REFRESH,
            Permission.TOKEN_LOGOUT
    ));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public Set<SimpleGrantedAuthority> getAuthorities() {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority(name()));

        for (Permission permission : permissions) {
            authorities.add(new SimpleGrantedAuthority(permission.getAuthority()));
        }

        return authorities;
    }

    public static Role fromValue(String value) {
        if (value != null) {
            for (Role role : values()) {
                if (role.name().equalsIgnoreCase(value)) {
                    return role;
                }
            }
        }

        return ROLE_USER;
    }
}

package com.creditrack.iam.domain.model;

public enum Permission {
    USER_READ("user:read"),
    USER_CREATE("user:create"),
    USER_UPDATE("user:update"),
    USER_DELETE("user:delete"),
    ROLE_READ("role:read"),
    ROLE_ASSIGN("role:assign"),
    PROFILE_READ("profile:read"),
    PROFILE_UPDATE("profile:update"),
    PASSWORD_CHANGE("password:change"),
    PASSWORD_RESET("password:reset"),
    TOKEN_REFRESH("token:refresh"),
    TOKEN_LOGOUT("token:logout");

    private final String authority;

    Permission(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return authority;
    }
}

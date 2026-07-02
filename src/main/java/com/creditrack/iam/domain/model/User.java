package com.creditrack.iam.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion = 0;

    @PrePersist
    void prePersist() {
        if (enabled == null) {
            enabled = true;
        }
        if (tokenVersion == null) {
            tokenVersion = 0;
        }
    }
}

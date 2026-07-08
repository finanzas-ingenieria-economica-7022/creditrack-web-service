package com.creditrack.iam.domain.repositories;

import com.creditrack.iam.domain.model.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {
    Optional<TokenBlacklist> findByJti(String jti);
    boolean existsByJtiAndExpiresAtAfter(String jti, LocalDateTime now);
    List<TokenBlacklist> findAllByUserId(Long userId);
    void deleteAllByExpiresAtBefore(LocalDateTime now);
    void deleteAllByUserId(Long userId);
}

package com.morsel.repository;

import com.morsel.model.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "UPDATE PasswordResetToken t SET t.used = true WHERE t.tokenHash = :hash AND t.used = false AND t.expiresAt > :now")
    int consumeToken(@Param("hash") String hash, @Param("now") Instant now);

    void deleteByUserId(Long userId);
}

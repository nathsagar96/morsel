package com.morsel.repository;

import com.morsel.model.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByJti(String jti);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.jti = :jti AND t.revoked = false")
    int markRevokedIfNotRevoked(@Param("jti") String jti);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.userId = :userId AND t.revoked = false")
    int revokeAllByUserId(@Param("userId") Long userId);
}

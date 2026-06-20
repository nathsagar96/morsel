package com.morsel.repository;

import com.morsel.model.RefreshToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByJti(String jti);

    List<RefreshToken> findAllByUserIdAndRevokedFalse(Long userId);
}

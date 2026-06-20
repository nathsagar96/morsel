package com.morsel.service;

import com.morsel.exception.ForbiddenException;
import com.morsel.model.RefreshToken;
import com.morsel.repository.RefreshTokenRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void create(Long userId, String jti, Instant expiresAt) {
        RefreshToken token = RefreshToken.builder()
                .jti(jti)
                .userId(userId)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
        refreshTokenRepository.save(token);
        log.debug("Stored refresh token jti={} for user {}", jti, userId);
    }

    @Transactional
    public void validateAndRotate(String jti, Long userId) {
        RefreshToken token = refreshTokenRepository.findByJti(jti).orElseThrow(() -> {
            log.warn("Refresh token jti={} not found in store", jti);
            return new ForbiddenException("Invalid refresh token");
        });

        if (token.isRevoked()) {
            log.warn("Refresh token reuse detected for user {} — revoking all tokens", userId);
            revokeAllForUser(userId);
            throw new ForbiddenException("Refresh token has been revoked");
        }

        token.setRevoked(true);
        refreshTokenRepository.save(token);
        log.debug("Revoked refresh token jti={}", jti);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        var tokens = refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId);
        if (!tokens.isEmpty()) {
            tokens.forEach(t -> t.setRevoked(true));
            refreshTokenRepository.saveAll(tokens);
        }
        log.debug("Revoked all refresh tokens for user {}", userId);
    }
}

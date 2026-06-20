package com.morsel.service;

import com.morsel.exception.UnauthorizedException;
import com.morsel.repository.RefreshTokenRepository;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TransactionTemplate requiresNewTransaction;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository, PlatformTransactionManager transactionManager) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public void create(Long userId, String jti, Instant expiresAt) {
        var token = com.morsel.model.RefreshToken.builder()
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
        if (refreshTokenRepository.markRevokedIfNotRevoked(jti) == 0) {
            log.warn("Refresh token reuse detected for user {} — revoking all tokens", userId);
            requiresNewTransaction.executeWithoutResult(_ -> refreshTokenRepository.revokeAllByUserId(userId));
            throw new UnauthorizedException("Refresh token has been revoked");
        }
        log.debug("Revoked refresh token jti={}", jti);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        int affected = refreshTokenRepository.revokeAllByUserId(userId);
        if (affected > 0) {
            log.debug("Revoked {} refresh tokens for user {}", affected, userId);
        }
    }
}

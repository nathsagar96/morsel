package com.morsel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.morsel.exception.UnauthorizedException;
import com.morsel.model.RefreshToken;
import com.morsel.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private static final Long USER_ID = 1L;
    private static final String JTI = UUID.randomUUID().toString();
    private static final Instant EXPIRES_AT = Instant.now().plusSeconds(3600);

    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        refreshToken = RefreshToken.builder()
                .id(1L)
                .jti(JTI)
                .userId(USER_ID)
                .expiresAt(EXPIRES_AT)
                .revoked(false)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("creates and persists a refresh token")
    void create_withValidArgs_savesToken() {
        refreshTokenService.create(USER_ID, JTI, EXPIRES_AT);

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("validateAndRotate revokes a non-revoked token")
    void validateAndRotate_withNonRevokedToken_marksAsRevoked() {
        when(refreshTokenRepository.findByJti(JTI)).thenReturn(Optional.of(refreshToken));

        refreshTokenService.validateAndRotate(JTI, USER_ID);

        assertThat(refreshToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    @DisplayName("validateAndRotate throws UnauthorizedException when token not found")
    void validateAndRotate_withUnknownJti_throwsUnauthorized() {
        when(refreshTokenRepository.findByJti("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validateAndRotate("unknown", USER_ID))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("validateAndRotate revokes all user tokens when reused token detected")
    void validateAndRotate_withRevokedToken_triggersFamilyRevocation() {
        RefreshToken revokedToken = RefreshToken.builder()
                .id(1L)
                .jti(JTI)
                .userId(USER_ID)
                .expiresAt(EXPIRES_AT)
                .revoked(true)
                .build();
        when(refreshTokenRepository.findByJti(JTI)).thenReturn(Optional.of(revokedToken));

        RefreshToken anotherToken = RefreshToken.builder()
                .id(2L)
                .jti("other-jti")
                .userId(USER_ID)
                .expiresAt(EXPIRES_AT)
                .revoked(false)
                .build();
        when(refreshTokenRepository.findAllByUserIdAndRevokedFalse(USER_ID)).thenReturn(List.of(anotherToken));

        assertThatThrownBy(() -> refreshTokenService.validateAndRotate(JTI, USER_ID))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("revoked");

        assertThat(anotherToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).saveAll(List.of(anotherToken));
    }

    @Test
    @DisplayName("revokeAllForUser revokes all non-revoked tokens for the user")
    void revokeAllForUser_withMultipleTokens_revokesAll() {
        RefreshToken token1 = RefreshToken.builder()
                .id(1L)
                .jti("jti-1")
                .userId(USER_ID)
                .expiresAt(EXPIRES_AT)
                .revoked(false)
                .build();
        RefreshToken token2 = RefreshToken.builder()
                .id(2L)
                .jti("jti-2")
                .userId(USER_ID)
                .expiresAt(EXPIRES_AT)
                .revoked(false)
                .build();
        when(refreshTokenRepository.findAllByUserIdAndRevokedFalse(USER_ID)).thenReturn(List.of(token1, token2));

        refreshTokenService.revokeAllForUser(USER_ID);

        assertThat(token1.isRevoked()).isTrue();
        assertThat(token2.isRevoked()).isTrue();
        verify(refreshTokenRepository).saveAll(List.of(token1, token2));
    }

    @Test
    @DisplayName("revokeAllForUser is a no-op when no non-revoked tokens exist")
    void revokeAllForUser_withNoTokens_doesNothing() {
        when(refreshTokenRepository.findAllByUserIdAndRevokedFalse(USER_ID)).thenReturn(List.of());

        refreshTokenService.revokeAllForUser(USER_ID);

        verify(refreshTokenRepository, never()).saveAll(any());
    }
}

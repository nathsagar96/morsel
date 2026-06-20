package com.morsel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.morsel.exception.UnauthorizedException;
import com.morsel.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private static final Long USER_ID = 1L;
    private static final String JTI = UUID.randomUUID().toString();
    private static final Instant EXPIRES_AT = Instant.now().plusSeconds(3600);

    @Test
    @DisplayName("creates and persists a refresh token")
    void create_withValidArgs_savesToken() {
        refreshTokenService.create(USER_ID, JTI, EXPIRES_AT);

        verify(refreshTokenRepository).save(any(com.morsel.model.RefreshToken.class));
    }

    @Test
    @DisplayName("validateAndRotate succeeds when token is not yet revoked")
    void validateAndRotate_withNonRevokedToken_succeeds() {
        when(refreshTokenRepository.markRevokedIfNotRevoked(JTI)).thenReturn(1);

        refreshTokenService.validateAndRotate(JTI, USER_ID);

        verify(refreshTokenRepository).markRevokedIfNotRevoked(JTI);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("validateAndRotate revokes all user tokens when token already consumed")
    void validateAndRotate_withAlreadyRevokedToken_triggersFamilyRevocation() {
        when(refreshTokenRepository.markRevokedIfNotRevoked(JTI)).thenReturn(0);
        when(refreshTokenRepository.revokeAllByUserId(USER_ID)).thenReturn(1);

        assertThatThrownBy(() -> refreshTokenService.validateAndRotate(JTI, USER_ID))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("revoked");

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(refreshTokenRepository).revokeAllByUserId(captor.capture());
        assertThat(captor.getValue()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("validateAndRotate throws UnauthorizedException when token not found")
    void validateAndRotate_withUnknownJti_throwsUnauthorized() {
        when(refreshTokenRepository.markRevokedIfNotRevoked("unknown")).thenReturn(0);
        when(refreshTokenRepository.revokeAllByUserId(USER_ID)).thenReturn(0);

        assertThatThrownBy(() -> refreshTokenService.validateAndRotate("unknown", USER_ID))
                .isInstanceOf(UnauthorizedException.class);

        verify(refreshTokenRepository).revokeAllByUserId(USER_ID);
    }

    @Test
    @DisplayName("revokeAllForUser revokes all non-revoked tokens for the user")
    void revokeAllForUser_withMultipleTokens_revokesAll() {
        when(refreshTokenRepository.revokeAllByUserId(USER_ID)).thenReturn(2);

        refreshTokenService.revokeAllForUser(USER_ID);

        verify(refreshTokenRepository).revokeAllByUserId(USER_ID);
    }

    @Test
    @DisplayName("revokeAllForUser is a no-op when no non-revoked tokens exist")
    void revokeAllForUser_withNoTokens_doesNothing() {
        when(refreshTokenRepository.revokeAllByUserId(USER_ID)).thenReturn(0);

        refreshTokenService.revokeAllForUser(USER_ID);

        verify(refreshTokenRepository).revokeAllByUserId(USER_ID);
    }
}

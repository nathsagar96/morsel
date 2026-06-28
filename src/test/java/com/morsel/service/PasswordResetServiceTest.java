package com.morsel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.morsel.config.PasswordResetProperties;
import com.morsel.event.PasswordResetEvent;
import com.morsel.exception.BadRequestException;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.model.PasswordResetToken;
import com.morsel.model.User;
import com.morsel.repository.PasswordResetTokenRepository;
import com.morsel.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService")
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PasswordResetProperties passwordResetProperties;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encoded-password")
                .build();
    }

    @Test
    @DisplayName("generates hashed token and publishes event for existing user")
    void initiatePasswordReset_withExistingUser_sendsEmail() {
        when(passwordResetProperties.tokenExpiryMinutes()).thenReturn(15);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        passwordResetService.initiatePasswordReset("test@example.com");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        PasswordResetToken savedToken = captor.getValue();
        assertThat(savedToken.getTokenHash()).isNotNull();
        assertThat(savedToken.getTokenHash()).hasSize(64);
        assertThat(savedToken.getUserId()).isEqualTo(1L);
        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());
        assertThat(savedToken.isUsed()).isFalse();
        ArgumentCaptor<PasswordResetEvent> eventCaptor = ArgumentCaptor.forClass(PasswordResetEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        PasswordResetEvent event = eventCaptor.getValue();
        assertThat(event.email()).isEqualTo("test@example.com");
        assertThat(event.token()).isNotNull();
    }

    @Test
    @DisplayName("does not publish event for non-existing user")
    void initiatePasswordReset_withNonExistingUser_doesNothing() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        passwordResetService.initiatePasswordReset("unknown@example.com");

        verify(passwordResetTokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("resets password with valid token via atomic consumption")
    void resetPassword_withValidToken_resetsPassword() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .tokenHash("a".repeat(64))
                .userId(1L)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .used(false)
                .build();
        when(passwordResetTokenRepository.consumeToken(any(String.class), any(Instant.class)))
                .thenReturn(1);
        when(passwordResetTokenRepository.findByTokenHash(any(String.class))).thenReturn(Optional.of(resetToken));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-encoded-password");

        passwordResetService.resetPassword("valid-token", "newPassword123");

        assertThat(user.getPassword()).isEqualTo("new-encoded-password");
        verify(userRepository).save(user);
        verify(refreshTokenService).revokeAllForUser(1L);
    }

    @Test
    @DisplayName("throws BadRequestException for non-existing token")
    void resetPassword_withInvalidToken_throwsBadRequest() {
        when(passwordResetTokenRepository.consumeToken(any(String.class), any(Instant.class)))
                .thenReturn(0);
        when(passwordResetTokenRepository.findByTokenHash(any(String.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("bad-token", "newPassword123"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    @DisplayName("throws BadRequestException for already used token")
    void resetPassword_withUsedToken_throwsBadRequest() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .tokenHash("a".repeat(64))
                .userId(1L)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .used(true)
                .build();
        when(passwordResetTokenRepository.consumeToken(any(String.class), any(Instant.class)))
                .thenReturn(0);
        when(passwordResetTokenRepository.findByTokenHash(any(String.class))).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> passwordResetService.resetPassword("used-token", "newPassword123"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    @DisplayName("throws BadRequestException for expired token")
    void resetPassword_withExpiredToken_throwsBadRequest() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .tokenHash("a".repeat(64))
                .userId(1L)
                .expiresAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .used(false)
                .build();
        when(passwordResetTokenRepository.consumeToken(any(String.class), any(Instant.class)))
                .thenReturn(0);
        when(passwordResetTokenRepository.findByTokenHash(any(String.class))).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> passwordResetService.resetPassword("expired-token", "newPassword123"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when user not found for token")
    void resetPassword_withOrphanedToken_throwsNotFound() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .tokenHash("a".repeat(64))
                .userId(999L)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .used(false)
                .build();
        when(passwordResetTokenRepository.consumeToken(any(String.class), any(Instant.class)))
                .thenReturn(1);
        when(passwordResetTokenRepository.findByTokenHash(any(String.class))).thenReturn(Optional.of(resetToken));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("valid-token", "newPassword123"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

package com.morsel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private EmailService emailService;

    @Mock
    private RefreshTokenService refreshTokenService;

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
    @DisplayName("generates token and sends email for existing user")
    void initiatePasswordReset_withExistingUser_sendsEmail() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        passwordResetService.initiatePasswordReset("test@example.com");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        PasswordResetToken savedToken = captor.getValue();
        assertThat(savedToken.getUserId()).isEqualTo(1L);
        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());
        assertThat(savedToken.isUsed()).isFalse();
        verify(emailService).sendPasswordResetEmail(eq("test@example.com"), any(String.class));
    }

    @Test
    @DisplayName("does not send email for non-existing user")
    void initiatePasswordReset_withNonExistingUser_doesNothing() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        passwordResetService.initiatePasswordReset("unknown@example.com");

        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    @DisplayName("resets password with valid token")
    void resetPassword_withValidToken_resetsPassword() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .token("valid-token")
                .userId(1L)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .used(false)
                .build();
        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(resetToken));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-encoded-password");

        passwordResetService.resetPassword("valid-token", "newPassword123");

        assertThat(user.getPassword()).isEqualTo("new-encoded-password");
        assertThat(resetToken.isUsed()).isTrue();
        verify(userRepository).save(user);
        verify(refreshTokenService).revokeAllForUser(1L);
    }

    @Test
    @DisplayName("throws BadRequestException for non-existing token")
    void resetPassword_withInvalidToken_throwsBadRequest() {
        when(passwordResetTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("bad-token", "newPassword123"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    @DisplayName("throws BadRequestException for already used token")
    void resetPassword_withUsedToken_throwsBadRequest() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .token("used-token")
                .userId(1L)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .used(true)
                .build();
        when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> passwordResetService.resetPassword("used-token", "newPassword123"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    @DisplayName("throws BadRequestException for expired token")
    void resetPassword_withExpiredToken_throwsBadRequest() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .token("expired-token")
                .userId(1L)
                .expiresAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .used(false)
                .build();
        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> passwordResetService.resetPassword("expired-token", "newPassword123"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when user not found for token")
    void resetPassword_withOrphanedToken_throwsNotFound() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .token("valid-token")
                .userId(999L)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .used(false)
                .build();
        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(resetToken));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("valid-token", "newPassword123"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

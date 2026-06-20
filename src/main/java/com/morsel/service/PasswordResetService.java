package com.morsel.service;

import com.morsel.exception.BadRequestException;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.logging.AuditLogger;
import com.morsel.logging.AuditLogger.Event;
import com.morsel.logging.AuditLogger.Outcome;
import com.morsel.model.PasswordResetToken;
import com.morsel.model.User;
import com.morsel.repository.PasswordResetTokenRepository;
import com.morsel.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public void initiatePasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUserId(user.getId());

            String token = generateSecureToken();
            String tokenHash = hashToken(token);
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .tokenHash(tokenHash)
                    .userId(user.getId())
                    .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                    .build();
            passwordResetTokenRepository.save(resetToken);
            log.debug("Password reset token generated for user {}", user.getId());
            AuditLogger.log(Event.PASSWORD_RESET_INITIATED, user.getId(), Outcome.SUCCESS);

            emailService.sendPasswordResetEmail(user.getEmail(), token);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String tokenHash = hashToken(token);
        int consumed = passwordResetTokenRepository.consumeToken(tokenHash, Instant.now());

        if (consumed == 0) {
            PasswordResetToken resetToken =
                    passwordResetTokenRepository.findByTokenHash(tokenHash).orElse(null);
            if (resetToken == null) {
                log.warn("Invalid password reset token");
                AuditLogger.log(Event.PASSWORD_RESET_FAILURE_INVALID_TOKEN, null, Outcome.FAILURE);
                throw new BadRequestException("Invalid or expired reset token");
            }
            if (resetToken.isUsed()) {
                log.warn("Password reset token already used");
                AuditLogger.log(Event.PASSWORD_RESET_FAILURE_USED_TOKEN, resetToken.getUserId(), Outcome.FAILURE);
                throw new BadRequestException("Reset token has already been used");
            }
            log.warn("Password reset token expired");
            AuditLogger.log(Event.PASSWORD_RESET_FAILURE_EXPIRED_TOKEN, resetToken.getUserId(), Outcome.FAILURE);
            throw new BadRequestException("Reset token has expired");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found after consume"));

        User user = userRepository.findById(resetToken.getUserId()).orElseThrow(() -> {
            log.warn("User not found for password reset, userId: {}", resetToken.getUserId());
            return new ResourceNotFoundException("User not found");
        });

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        refreshTokenService.revokeAllForUser(user.getId());
        log.debug("Password reset completed for user {}", user.getId());
        AuditLogger.log(Event.PASSWORD_RESET_COMPLETED, user.getId(), Outcome.SUCCESS);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}

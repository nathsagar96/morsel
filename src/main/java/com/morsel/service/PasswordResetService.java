package com.morsel.service;

import com.morsel.exception.BadRequestException;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.model.PasswordResetToken;
import com.morsel.model.User;
import com.morsel.repository.PasswordResetTokenRepository;
import com.morsel.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
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
            // Invalidate any existing reset tokens for this user
            passwordResetTokenRepository.deleteByUserId(user.getId());

            String token = generateSecureToken();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .userId(user.getId())
                    .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                    .build();
            passwordResetTokenRepository.save(resetToken);
            log.debug("Password reset token generated for user {}", user.getId());

            emailService.sendPasswordResetEmail(user.getEmail(), token);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Invalid password reset token");
                    return new BadRequestException("Invalid or expired reset token");
                });

        if (resetToken.isUsed()) {
            log.warn("Password reset token already used: {}", token);
            throw new BadRequestException("Reset token has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Password reset token expired: {}", token);
            throw new BadRequestException("Reset token has expired");
        }

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        User user = userRepository.findById(resetToken.getUserId()).orElseThrow(() -> {
            log.warn("User not found for password reset, userId: {}", resetToken.getUserId());
            return new ResourceNotFoundException("User not found");
        });

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Revoke all refresh tokens for security — force re-login everywhere
        refreshTokenService.revokeAllForUser(user.getId());
        log.debug("Password reset completed for user {}", user.getId());
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}

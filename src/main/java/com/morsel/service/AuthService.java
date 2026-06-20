package com.morsel.service;

import com.morsel.config.JwtProperties;
import com.morsel.config.LockoutProperties;
import com.morsel.dto.request.LoginRequest;
import com.morsel.dto.request.RefreshTokenRequest;
import com.morsel.dto.request.SignUpRequest;
import com.morsel.dto.response.AuthResponse;
import com.morsel.exception.AccountDisabledException;
import com.morsel.exception.AccountLockedException;
import com.morsel.exception.DuplicateResourceException;
import com.morsel.exception.UnauthorizedException;
import com.morsel.logging.AuditLogger;
import com.morsel.logging.AuditLogger.Event;
import com.morsel.logging.AuditLogger.Outcome;
import com.morsel.logging.PiiSanitizer;
import com.morsel.mapper.UserMapper;
import com.morsel.model.Role;
import com.morsel.model.User;
import com.morsel.repository.UserRepository;
import com.morsel.security.JwtTokenProvider;
import com.morsel.security.UserPrincipal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;
    private final LockoutProperties lockoutProperties;

    @Transactional
    public AuthResponse register(SignUpRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            log.debug(
                    "Registration failed: username {} already exists",
                    PiiSanitizer.sanitizeIdentifier(request.username()));
            AuditLogger.log(
                    Event.REGISTRATION_FAILURE_DUPLICATE_USERNAME,
                    null,
                    Outcome.FAILURE,
                    "username=" + PiiSanitizer.sanitizeUsername(request.username()));
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            log.debug("Registration failed: email {} already exists", PiiSanitizer.sanitizeIdentifier(request.email()));
            AuditLogger.log(
                    Event.REGISTRATION_FAILURE_DUPLICATE_EMAIL,
                    null,
                    Outcome.FAILURE,
                    "email=" + PiiSanitizer.sanitizeEmail(request.email()));
            throw new DuplicateResourceException("Email already exists");
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = userMapper.toEntity(request, encodedPassword, Role.USER);
        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            log.warn("Registration failed due to constraint violation: {}", e.getMessage());
            AuditLogger.log(
                    Event.REGISTRATION_FAILURE_CONSTRAINT_VIOLATION,
                    null,
                    Outcome.FAILURE,
                    "constraint=" + e.getMessage());
            throw new DuplicateResourceException("Username or email already exists");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getEmail());
        String jti = UUID.randomUUID().toString();
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), jti);
        refreshTokenService.create(user.getId(), jti, Instant.now().plusMillis(jwtProperties.refreshExpirationMs()));
        log.debug("User registered: {}", PiiSanitizer.sanitizeUsername(request.username()));
        AuditLogger.log(
                Event.REGISTRATION_SUCCESS,
                user.getId(),
                Outcome.SUCCESS,
                "username=" + PiiSanitizer.sanitizeUsername(user.getUsername()));
        return userMapper.toAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthResponse authenticate(LoginRequest request) {
        User user = userRepository
                .findByUsername(request.usernameOrEmail())
                .or(() -> userRepository.findByEmail(request.usernameOrEmail()))
                .orElse(null);

        if (user != null) {
            if (!user.isAccountNonLocked()) {
                if (user.getLockTime() != null
                        && user.getLockTime()
                                .plus(lockoutProperties.lockDurationMinutes(), ChronoUnit.MINUTES)
                                .isBefore(Instant.now())) {
                    log.debug(
                            "Lock duration expired for user {}, unlocking",
                            PiiSanitizer.sanitizeUsername(user.getUsername()));
                    AuditLogger.log(
                            Event.ACCOUNT_UNLOCKED,
                            user.getId(),
                            Outcome.SUCCESS,
                            "lockDurationMinutes=" + lockoutProperties.lockDurationMinutes());
                    user.setAccountNonLocked(true);
                    user.setFailedAttempts(0);
                    user.setLockTime(null);
                } else {
                    log.debug("Account locked for user {}", PiiSanitizer.sanitizeUsername(user.getUsername()));
                    throw new AccountLockedException("Too many failed login attempts. Account is locked for %d minutes"
                            .formatted(lockoutProperties.lockDurationMinutes()));
                }
            }
            if (!user.isEnabled()) {
                log.debug("Account disabled for user {}", PiiSanitizer.sanitizeUsername(user.getUsername()));
                throw new AccountDisabledException("Account is disabled");
            }
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.usernameOrEmail(), request.password()));

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            User authenticatedUser = principal.user();

            // Successful login — reset failed attempts
            if (authenticatedUser.getFailedAttempts() > 0) {
                authenticatedUser.setFailedAttempts(0);
                authenticatedUser.setLockTime(null);
            }

            String accessToken = jwtTokenProvider.generateAccessToken(
                    authenticatedUser.getId(), authenticatedUser.getUsername(), authenticatedUser.getEmail());
            String jti = UUID.randomUUID().toString();
            String refreshToken = jwtTokenProvider.generateRefreshToken(authenticatedUser.getId(), jti);
            refreshTokenService.create(
                    authenticatedUser.getId(), jti, Instant.now().plusMillis(jwtProperties.refreshExpirationMs()));
            log.debug("User signed in: {}", PiiSanitizer.sanitizeUsername(authenticatedUser.getUsername()));
            AuditLogger.log(Event.LOGIN_SUCCESS, authenticatedUser.getId(), Outcome.SUCCESS);
            return userMapper.toAuthResponse(authenticatedUser, accessToken, refreshToken);
        } catch (BadCredentialsException e) {
            if (user != null) {
                int newAttempts = user.getFailedAttempts() + 1;
                user.setFailedAttempts(newAttempts);
                if (newAttempts >= lockoutProperties.maxFailedAttempts()) {
                    user.setAccountNonLocked(false);
                    user.setLockTime(Instant.now());
                    log.warn(
                            "Account locked after {} failed attempts for user {}",
                            newAttempts,
                            PiiSanitizer.sanitizeUsername(user.getUsername()));
                    AuditLogger.log(
                            Event.ACCOUNT_LOCKED, user.getId(), Outcome.SUCCESS, "failedAttempts=" + newAttempts);
                } else {
                    log.debug(
                            "Failed login attempt {} for user {}",
                            newAttempts,
                            PiiSanitizer.sanitizeUsername(user.getUsername()));
                }
            }
            AuditLogger.log(Event.LOGIN_FAILURE, user != null ? user.getId() : null, Outcome.FAILURE, "badCredentials");
            throw e;
        }
    }

    public AuthResponse refreshAccessToken(RefreshTokenRequest request) {
        var claims = jwtTokenProvider
                .validateRefreshToken(request.refreshToken())
                .orElseThrow(() -> {
                    log.warn("Refresh token validation failed");
                    AuditLogger.log(Event.TOKEN_REFRESH_FAILURE, null, Outcome.FAILURE, "invalidToken");
                    return new UnauthorizedException("Invalid or expired refresh token");
                });

        refreshTokenService.validateAndRotate(claims.jti(), claims.userId());

        User user = userRepository.findById(claims.userId()).orElseThrow(() -> {
            log.warn("User not found for refresh token, userId: {}", claims.userId());
            AuditLogger.log(Event.TOKEN_REFRESH_FAILURE, claims.userId(), Outcome.FAILURE, "userNotFound");
            return new UnauthorizedException("Invalid or expired refresh token");
        });

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getEmail());
        String newJti = UUID.randomUUID().toString();
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), newJti);
        refreshTokenService.create(user.getId(), newJti, Instant.now().plusMillis(jwtProperties.refreshExpirationMs()));
        log.debug("Tokens refreshed for user {}", PiiSanitizer.sanitizeUsername(user.getUsername()));
        AuditLogger.log(Event.TOKEN_REFRESH_SUCCESS, user.getId(), Outcome.SUCCESS);
        return userMapper.toAuthResponse(user, newAccessToken, newRefreshToken);
    }
}

package com.morsel.service;

import com.morsel.config.JwtProperties;
import com.morsel.dto.request.LoginRequest;
import com.morsel.dto.request.RefreshTokenRequest;
import com.morsel.dto.request.SignUpRequest;
import com.morsel.dto.response.AuthResponse;
import com.morsel.exception.DuplicateResourceException;
import com.morsel.exception.ForbiddenException;
import com.morsel.mapper.UserMapper;
import com.morsel.model.Role;
import com.morsel.model.User;
import com.morsel.repository.UserRepository;
import com.morsel.security.JwtTokenProvider;
import com.morsel.security.UserPrincipal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
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

    @Transactional
    public AuthResponse register(SignUpRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            log.debug("Registration failed: username {} already exists", request.username());
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            log.debug("Registration failed: email {} already exists", request.email());
            throw new DuplicateResourceException("Email already exists");
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = userMapper.toEntity(request, encodedPassword, Role.USER);
        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            log.warn("Registration failed due to constraint violation: {}", e.getMessage());
            throw new DuplicateResourceException("Username or email already exists");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String jti = UUID.randomUUID().toString();
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), jti);
        refreshTokenService.create(
                user.getId(), jti, java.time.Instant.now().plusMillis(jwtProperties.refreshExpirationMs()));
        log.debug("User registered: {}", request.username());
        return userMapper.toAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse authenticate(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.usernameOrEmail(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = principal.user();
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String jti = UUID.randomUUID().toString();
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), jti);
        refreshTokenService.create(
                user.getId(), jti, java.time.Instant.now().plusMillis(jwtProperties.refreshExpirationMs()));
        log.debug("User signed in: {}", user.getUsername());
        return userMapper.toAuthResponse(user, accessToken, refreshToken);
    }

    public AuthResponse refreshAccessToken(RefreshTokenRequest request) {
        var claims = jwtTokenProvider
                .validateRefreshToken(request.refreshToken())
                .orElseThrow(() -> {
                    log.warn("Refresh token validation failed");
                    return new ForbiddenException("Invalid or expired refresh token");
                });

        refreshTokenService.validateAndRotate(claims.jti(), claims.userId());

        User user = userRepository.findById(claims.userId()).orElseThrow(() -> {
            log.warn("User not found for refresh token, userId: {}", claims.userId());
            return new ForbiddenException("Invalid or expired refresh token");
        });

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String newJti = UUID.randomUUID().toString();
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), newJti);
        refreshTokenService.create(
                user.getId(), newJti, java.time.Instant.now().plusMillis(jwtProperties.refreshExpirationMs()));
        log.debug("Tokens refreshed for user {}", user.getUsername());
        return userMapper.toAuthResponse(user, newAccessToken, newRefreshToken);
    }
}

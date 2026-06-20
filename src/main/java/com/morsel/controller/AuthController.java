package com.morsel.controller;

import com.morsel.dto.request.ForgotPasswordRequest;
import com.morsel.dto.request.LoginRequest;
import com.morsel.dto.request.RefreshTokenRequest;
import com.morsel.dto.request.ResetPasswordRequest;
import com.morsel.dto.request.SignUpRequest;
import com.morsel.dto.response.AuthResponse;
import com.morsel.service.AuthService;
import com.morsel.service.PasswordResetService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody SignUpRequest request) {
        log.debug("Signup request for user: {}", request.username());
        return authService.register(request);
    }

    @PostMapping("/signin")
    public AuthResponse signin(@Valid @RequestBody LoginRequest request) {
        log.debug("Signin request for user: {}", request.usernameOrEmail());
        return authService.authenticate(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("Refresh token request received");
        return authService.refreshAccessToken(request);
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.debug("Password reset requested for email: {}", request.email());
        passwordResetService.initiatePasswordReset(request.email());
        return Map.of("message", "If the email exists, a password reset link has been sent");
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.debug("Password reset attempt with token");
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return Map.of("message", "Password has been reset successfully");
    }
}

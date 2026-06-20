package com.morsel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.morsel.config.JwtProperties;
import com.morsel.config.LockoutProperties;
import com.morsel.dto.request.LoginRequest;
import com.morsel.dto.request.RefreshTokenRequest;
import com.morsel.dto.request.SignUpRequest;
import com.morsel.dto.response.AuthResponse;
import com.morsel.exception.AccountDisabledException;
import com.morsel.exception.AccountLockedException;
import com.morsel.exception.DuplicateResourceException;
import com.morsel.exception.ForbiddenException;
import com.morsel.mapper.UserMapper;
import com.morsel.model.Role;
import com.morsel.model.User;
import com.morsel.repository.UserRepository;
import com.morsel.security.JwtTokenProvider;
import com.morsel.security.UserPrincipal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private LockoutProperties lockoutProperties;

    @InjectMocks
    private AuthService authService;

    private SignUpRequest signUpRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        signUpRequest = new SignUpRequest("newuser", "new@example.com", "password123");
        loginRequest = new LoginRequest("testuser", "password");
        user = User.builder()
                .id(1L)
                .username("newuser")
                .email("new@example.com")
                .password("encoded-password")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("registers user and returns auth response with token")
    void register_withValidRequest_returnsAuthResponse() {
        when(jwtProperties.refreshExpirationMs()).thenReturn(604800000L);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userMapper.toEntity(signUpRequest, "encoded-password", Role.USER)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(jwtTokenProvider.generateAccessToken(anyLong(), any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn("test-token");
        when(jwtTokenProvider.generateRefreshToken(eq(1L), any(String.class))).thenReturn("test-refresh");
        when(userMapper.toAuthResponse(user, "test-token", "test-refresh"))
                .thenReturn(AuthResponse.of("test-token", "test-refresh", 1L, "newuser", "new@example.com"));

        AuthResponse response = authService.register(signUpRequest);

        assertThat(response.token()).isEqualTo("test-token");
        assertThat(response.refreshToken()).isEqualTo("test-refresh");
        assertThat(response.username()).isEqualTo("newuser");
        assertThat(response.email()).isEqualTo("new@example.com");
        verify(userRepository).save(user);
        verify(refreshTokenService).create(eq(1L), any(String.class), any(Instant.class));
    }

    @Test
    @DisplayName("throws DuplicateResourceException when username taken")
    void register_withDuplicateUsername_throwsException() {
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(signUpRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws DuplicateResourceException when email taken")
    void register_withDuplicateEmail_throwsException() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(signUpRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws DuplicateResourceException on database constraint violation")
    void register_withConstraintViolation_throwsException() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userMapper.toEntity(signUpRequest, "encoded-password", Role.USER)).thenReturn(user);
        when(userRepository.save(user)).thenThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> authService.register(signUpRequest)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("authenticates user and returns auth response with token")
    void authenticate_withValidCredentials_returnsAuthResponse() {
        when(jwtProperties.refreshExpirationMs()).thenReturn(604800000L);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        Authentication authentication = mock(Authentication.class);
        UserPrincipal userPrincipal = new UserPrincipal(user);
        when(authenticationManager.authenticate(new UsernamePasswordAuthenticationToken("testuser", "password")))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(jwtTokenProvider.generateAccessToken(anyLong(), any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn("test-token");
        when(jwtTokenProvider.generateRefreshToken(eq(1L), any(String.class))).thenReturn("test-refresh");
        when(userMapper.toAuthResponse(user, "test-token", "test-refresh"))
                .thenReturn(AuthResponse.of("test-token", "test-refresh", 1L, "newuser", "new@example.com"));

        AuthResponse response = authService.authenticate(loginRequest);

        assertThat(response.token()).isEqualTo("test-token");
        assertThat(response.refreshToken()).isEqualTo("test-refresh");
        assertThat(response.username()).isEqualTo("newuser");
        verify(authenticationManager).authenticate(any());
        verify(refreshTokenService).create(eq(1L), any(String.class), any(Instant.class));
    }

    @Test
    @DisplayName("increments failed attempts on bad credentials")
    void authenticate_withBadCredentials_incrementsFailedAttempts() {
        when(lockoutProperties.maxFailedAttempts()).thenReturn(5);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.authenticate(loginRequest)).isInstanceOf(BadCredentialsException.class);

        assertThat(user.getFailedAttempts()).isEqualTo(1);
        assertThat(user.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("locks account after max failed attempts")
    void authenticate_withMaxFailedAttempts_locksAccount() {
        when(lockoutProperties.maxFailedAttempts()).thenReturn(3);
        user.setFailedAttempts(2);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.authenticate(loginRequest)).isInstanceOf(BadCredentialsException.class);

        assertThat(user.getFailedAttempts()).isEqualTo(3);
        assertThat(user.isAccountNonLocked()).isFalse();
        assertThat(user.getLockTime()).isNotNull();
    }

    @Test
    @DisplayName("throws AccountLockedException when account is locked")
    void authenticate_whenAccountLocked_throwsAccountLockedException() {
        when(lockoutProperties.lockDurationMinutes()).thenReturn(15);
        user.setAccountNonLocked(false);
        user.setLockTime(Instant.now());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.authenticate(loginRequest))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("locked");

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("auto-unlocks account when lock duration has expired")
    void authenticate_whenLockExpired_unlocksAccount() {
        when(jwtProperties.refreshExpirationMs()).thenReturn(604800000L);
        when(lockoutProperties.lockDurationMinutes()).thenReturn(15);
        user.setAccountNonLocked(false);
        user.setLockTime(Instant.now().minus(20, java.time.temporal.ChronoUnit.MINUTES));
        user.setFailedAttempts(5);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        Authentication authentication = mock(Authentication.class);
        UserPrincipal userPrincipal = new UserPrincipal(user);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(jwtTokenProvider.generateAccessToken(anyLong(), any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn("test-token");
        when(jwtTokenProvider.generateRefreshToken(eq(1L), any(String.class))).thenReturn("test-refresh");
        when(userMapper.toAuthResponse(user, "test-token", "test-refresh"))
                .thenReturn(AuthResponse.of("test-token", "test-refresh", 1L, "testuser", "test@example.com"));

        AuthResponse response = authService.authenticate(loginRequest);

        assertThat(response.token()).isEqualTo("test-token");
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.getFailedAttempts()).isEqualTo(0);
        assertThat(user.getLockTime()).isNull();
    }

    @Test
    @DisplayName("throws AccountDisabledException when account is disabled")
    void authenticate_whenAccountDisabled_throwsAccountDisabledException() {
        user.setEnabled(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.authenticate(loginRequest))
                .isInstanceOf(AccountDisabledException.class)
                .hasMessageContaining("disabled");

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("resets failed attempts on successful login")
    void authenticate_withSuccessfulLogin_resetsFailedAttempts() {
        when(jwtProperties.refreshExpirationMs()).thenReturn(604800000L);
        user.setFailedAttempts(3);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        Authentication authentication = mock(Authentication.class);
        UserPrincipal userPrincipal = new UserPrincipal(user);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(jwtTokenProvider.generateAccessToken(anyLong(), any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn("test-token");
        when(jwtTokenProvider.generateRefreshToken(eq(1L), any(String.class))).thenReturn("test-refresh");
        when(userMapper.toAuthResponse(user, "test-token", "test-refresh"))
                .thenReturn(AuthResponse.of("test-token", "test-refresh", 1L, "testuser", "test@example.com"));

        authService.authenticate(loginRequest);

        assertThat(user.getFailedAttempts()).isEqualTo(0);
        assertThat(user.getLockTime()).isNull();
    }

    @Test
    @DisplayName("refreshes access token with valid refresh token")
    void refreshAccessToken_withValidToken_returnsNewTokens() {
        when(jwtProperties.refreshExpirationMs()).thenReturn(604800000L);
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        JwtTokenProvider.RefreshTokenClaims claims = new JwtTokenProvider.RefreshTokenClaims(
                1L, "jti-old", Instant.now().plusSeconds(3600));
        when(jwtTokenProvider.validateRefreshToken("valid-refresh-token")).thenReturn(Optional.of(claims));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(anyLong(), any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(eq(1L), any(String.class))).thenReturn("new-refresh-token");
        when(userMapper.toAuthResponse(user, "new-access-token", "new-refresh-token"))
                .thenReturn(AuthResponse.of("new-access-token", "new-refresh-token", 1L, "newuser", "new@example.com"));

        AuthResponse response = authService.refreshAccessToken(request);

        assertThat(response.token()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenService).validateAndRotate("jti-old", 1L);
        verify(refreshTokenService).create(eq(1L), any(String.class), any(Instant.class));
    }

    @Test
    @DisplayName("throws ForbiddenException for invalid refresh token")
    void refreshAccessToken_withInvalidToken_throwsForbidden() {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid");
        when(jwtTokenProvider.validateRefreshToken("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshAccessToken(request)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("throws ForbiddenException when user not found for refresh token")
    void refreshAccessToken_withUserNotFound_throwsForbidden() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-token-no-user");
        JwtTokenProvider.RefreshTokenClaims claims = new JwtTokenProvider.RefreshTokenClaims(
                999L, "jti-999", Instant.now().plusSeconds(3600));
        when(jwtTokenProvider.validateRefreshToken("valid-token-no-user")).thenReturn(Optional.of(claims));
        doNothing().when(refreshTokenService).validateAndRotate("jti-999", 999L);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshAccessToken(request)).isInstanceOf(ForbiddenException.class);
    }
}

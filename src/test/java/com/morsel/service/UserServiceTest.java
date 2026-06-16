package com.morsel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.morsel.dto.request.LoginRequest;
import com.morsel.dto.request.SignUpRequest;
import com.morsel.dto.response.AuthResponse;
import com.morsel.exception.DuplicateResourceException;
import com.morsel.mapper.UserMapper;
import com.morsel.model.Role;
import com.morsel.model.User;
import com.morsel.repository.UserRepository;
import com.morsel.security.JwtTokenProvider;
import com.morsel.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

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

    @InjectMocks
    private UserService userService;

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
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userMapper.toEntity(signUpRequest, "encoded-password", Role.USER)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(jwtTokenProvider.generateToken(1L)).thenReturn("test-token");
        when(userMapper.toAuthResponse(user, "test-token"))
                .thenReturn(AuthResponse.of("test-token", 1L, "newuser", "new@example.com"));

        AuthResponse response = userService.register(signUpRequest);

        assertThat(response.token()).isEqualTo("test-token");
        assertThat(response.username()).isEqualTo("newuser");
        assertThat(response.email()).isEqualTo("new@example.com");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("throws DuplicateResourceException when username taken")
    void register_withDuplicateUsername_throwsException() {
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(signUpRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws DuplicateResourceException when email taken")
    void register_withDuplicateEmail_throwsException() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(signUpRequest))
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

        assertThatThrownBy(() -> userService.register(signUpRequest)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("authenticates user and returns auth response with token")
    void authenticate_withValidCredentials_returnsAuthResponse() {
        Authentication authentication = mock(Authentication.class);
        UserPrincipal userPrincipal = new UserPrincipal(user);
        when(authenticationManager.authenticate(new UsernamePasswordAuthenticationToken("testuser", "password")))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(jwtTokenProvider.generateToken(1L)).thenReturn("test-token");
        when(userMapper.toAuthResponse(user, "test-token"))
                .thenReturn(AuthResponse.of("test-token", 1L, "newuser", "new@example.com"));

        AuthResponse response = userService.authenticate(loginRequest);

        assertThat(response.token()).isEqualTo("test-token");
        assertThat(response.username()).isEqualTo("newuser");
        verify(authenticationManager).authenticate(any());
    }
}

package com.morsel.service;

import com.morsel.dto.request.LoginRequest;
import com.morsel.dto.request.SignUpRequest;
import com.morsel.dto.response.AuthResponse;
import com.morsel.dto.response.UserProfileResponse;
import com.morsel.exception.DuplicateResourceException;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.mapper.UserMapper;
import com.morsel.model.Role;
import com.morsel.model.User;
import com.morsel.repository.UserRepository;
import com.morsel.security.JwtTokenProvider;
import com.morsel.security.UserPrincipal;
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
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse register(SignUpRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            log.warn("Registration failed: username {} already exists", request.username());
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            log.warn("Registration failed: email {} already exists", request.email());
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

        String token = jwtTokenProvider.generateToken(user.getId());
        log.info("User registered: {}", request.username());
        return userMapper.toAuthResponse(user, token);
    }

    public AuthResponse authenticate(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.usernameOrEmail(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = principal.user();
        String token = jwtTokenProvider.generateToken(user.getId());
        log.info("User signed in: {}", user.getUsername());
        return userMapper.toAuthResponse(user, token);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username) {
        User user = userRepository.findWithRecipesByUsername(username).orElseThrow(() -> {
            log.warn("User profile not found: {}", username);
            return new ResourceNotFoundException("User not found: " + username);
        });
        log.debug("Profile fetched for user: {}", username);
        return userMapper.toProfileResponse(user);
    }
}

package com.morsel.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.morsel.security.JwtTokenProvider.AccessTokenClaims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private FilterChain filterChain;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filterChain = mock(FilterChain.class);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("sets authentication for valid JWT with active user claims")
    void doFilterInternal_withValidTokenAndActiveClaims_setsAuthentication() throws ServletException, IOException {
        String token = "valid-token";
        request.addHeader("Authorization", "Bearer " + token);
        AccessTokenClaims claims = new AccessTokenClaims(1L, "testuser", "USER", true, true);
        when(jwtTokenProvider.extractAccessTokenClaims(token)).thenReturn(Optional.of(claims));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isInstanceOf(UserPrincipal.class);
        assertThat(((UserPrincipal) SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getPrincipal())
                        .user()
                        .getId())
                .isEqualTo(1L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("rejects JWT for disabled user claims")
    void doFilterInternal_withDisabledClaims_clearsSecurityContext() throws ServletException, IOException {
        String token = "valid-token";
        request.addHeader("Authorization", "Bearer " + token);
        AccessTokenClaims claims = new AccessTokenClaims(1L, "testuser", "USER", false, true);
        when(jwtTokenProvider.extractAccessTokenClaims(token)).thenReturn(Optional.of(claims));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("rejects JWT for locked user claims")
    void doFilterInternal_withLockedClaims_clearsSecurityContext() throws ServletException, IOException {
        String token = "valid-token";
        request.addHeader("Authorization", "Bearer " + token);
        AccessTokenClaims claims = new AccessTokenClaims(1L, "testuser", "USER", true, false);
        when(jwtTokenProvider.extractAccessTokenClaims(token)).thenReturn(Optional.of(claims));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("clears context for invalid JWT")
    void doFilterInternal_withInvalidToken_clearsSecurityContext() throws ServletException, IOException {
        String token = "invalid-token";
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtTokenProvider.extractAccessTokenClaims(token)).thenReturn(Optional.empty());

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("proceeds without authentication when no token present")
    void doFilterInternal_withNoToken_proceedsWithoutAuth() throws ServletException, IOException {
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("proceeds without authentication for malformed Authorization header")
    void doFilterInternal_withMalformedHeader_proceedsWithoutAuth() throws ServletException, IOException {
        request.addHeader("Authorization", "Basic abc123");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}

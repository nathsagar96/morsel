package com.morsel.security;

import com.morsel.config.logging.AuditLogger;
import com.morsel.config.logging.AuditLogger.Event;
import com.morsel.config.logging.AuditLogger.Outcome;
import com.morsel.constants.AuthConstants;
import com.morsel.model.Role;
import com.morsel.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = extractToken(request);

            if (token == null) {
                log.trace("No JWT token found in request");
            } else {
                Optional<JwtTokenProvider.AccessTokenClaims> claims =
                        jwtTokenProvider.validateAccessTokenWithClaims(token);
                if (claims.isPresent()) {
                    JwtTokenProvider.AccessTokenClaims c = claims.get();

                    if (!c.enabled() || !c.accountNonLocked()) {
                        log.warn("User {} is disabled or locked, rejecting JWT", c.userId());
                        AuditLogger.log(
                                Event.JWT_REJECTED_DISABLED_LOCKED,
                                c.userId(),
                                Outcome.FAILURE,
                                "enabled=" + c.enabled() + ",accountNonLocked=" + c.accountNonLocked());
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }

                    UserPrincipal principal = buildPrincipal(c);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authenticated user {} with JWT", c.userId());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to authenticate request", e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private UserPrincipal buildPrincipal(JwtTokenProvider.AccessTokenClaims c) {
        User user = User.builder()
                .id(c.userId())
                .username(c.username())
                .email(c.email())
                .role(Role.valueOf(c.role()))
                .enabled(c.enabled())
                .accountNonLocked(c.accountNonLocked())
                .build();
        return new UserPrincipal(user);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AuthConstants.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(AuthConstants.BEARER_PREFIX)) {
            return bearerToken.substring(AuthConstants.BEARER_PREFIX_LENGTH);
        }
        return null;
    }
}

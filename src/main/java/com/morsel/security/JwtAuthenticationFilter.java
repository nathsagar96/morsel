package com.morsel.security;

import com.morsel.constants.AuthConstants;
import com.morsel.logging.AuditLogger;
import com.morsel.logging.AuditLogger.Event;
import com.morsel.logging.AuditLogger.Outcome;
import com.morsel.security.JwtTokenProvider.AccessTokenClaims;
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
                Optional<AccessTokenClaims> claimsOpt = jwtTokenProvider.extractAccessTokenClaims(token);

                if (claimsOpt.isPresent()) {
                    AccessTokenClaims claims = claimsOpt.get();

                    if (!claims.enabled() || !claims.accountNonLocked()) {
                        log.warn("User {} is disabled or locked, rejecting JWT", claims.userId());
                        AuditLogger.log(
                                Event.JWT_REJECTED_DISABLED_LOCKED,
                                claims.userId(),
                                Outcome.FAILURE,
                                "enabled=" + claims.enabled() + ",accountNonLocked=" + claims.accountNonLocked());
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }

                    UserPrincipal principal = UserPrincipal.fromClaims(claims);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authenticated user {} from JWT claims", claims.userId());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to authenticate request", e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AuthConstants.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(AuthConstants.BEARER_PREFIX)) {
            return bearerToken.substring(AuthConstants.BEARER_PREFIX_LENGTH);
        }
        return null;
    }
}

package com.morsel.security;

import com.morsel.constants.AuthConstants;
import com.morsel.logging.AuditLogger;
import com.morsel.logging.AuditLogger.Event;
import com.morsel.logging.AuditLogger.Outcome;
import com.morsel.model.User;
import com.morsel.repository.UserRepository;
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
    private final UserRepository userRepository;

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
                Optional<Long> userIdOpt = jwtTokenProvider.validateAccessToken(token);

                if (userIdOpt.isPresent()) {
                    Long userId = userIdOpt.get();

                    User user = userRepository.findById(userId).orElse(null);

                    if (user == null) {
                        log.warn("User {} not found, rejecting JWT", userId);
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }

                    if (!user.isEnabled() || !user.isAccountNonLocked()) {
                        log.warn("User {} is disabled or locked, rejecting JWT", userId);
                        AuditLogger.log(
                                Event.JWT_REJECTED_DISABLED_LOCKED,
                                userId,
                                Outcome.FAILURE,
                                "enabled=" + user.isEnabled() + ",accountNonLocked=" + user.isAccountNonLocked());
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }

                    UserPrincipal principal = new UserPrincipal(user);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authenticated user {} from DB lookup", userId);
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

package com.morsel.security;

import com.morsel.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtTokenProvider {

    private static final String CLAIM_TYPE = "typ";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
    }

    public String generateAccessToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.expirationMs());

        log.debug("Generating access token for user {}", userId);
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .issuer(jwtProperties.issuer())
                .audience()
                .add(jwtProperties.audience())
                .and()
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Long userId, String jti) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.refreshExpirationMs());

        log.debug("Generating refresh token for user {} with jti {}", userId, jti);
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
                .issuer(jwtProperties.issuer())
                .audience()
                .add(jwtProperties.audience())
                .and()
                .id(jti)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Optional<Long> validateAccessToken(String token) {
        try {
            Claims claims = parseToken(token);
            if (!TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
                log.debug("Token is not an access token");
                return Optional.empty();
            }
            return Optional.of(Long.parseLong(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid access JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<RefreshTokenClaims> validateRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            if (!TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
                log.debug("Token is not a refresh token");
                return Optional.empty();
            }
            Long userId = Long.parseLong(claims.getSubject());
            String jti = claims.getId();
            return Optional.of(
                    new RefreshTokenClaims(userId, jti, claims.getExpiration().toInstant()));
        } catch (ExpiredJwtException e) {
            log.debug("Refresh token expired");
            return Optional.empty();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid refresh JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(jwtProperties.issuer())
                .requireAudience(jwtProperties.audience())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public record RefreshTokenClaims(Long userId, String jti, Instant expiresAt) {}
}

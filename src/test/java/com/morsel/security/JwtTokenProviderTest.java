package com.morsel.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.morsel.config.JwtProperties;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {

    private static final String TEST_SECRET = "VQxKJyDLXKDmK8JJeiLnH03vPoNWX0j2ruOlIS5xNMc=";
    private static final long EXPIRATION_MS = 3600000;
    private static final long REFRESH_EXPIRATION_MS = 604800000;
    private static final String ISSUER = "morsel";
    private static final String AUDIENCE = "morsel-api";

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties =
                new JwtProperties(TEST_SECRET, EXPIRATION_MS, REFRESH_EXPIRATION_MS, ISSUER, AUDIENCE);
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    @Test
    @DisplayName("generates valid access JWT with three parts")
    void generateAccessToken_returnsValidToken() {
        String token = jwtTokenProvider.generateAccessToken(1L, "testuser", "test@example.com");

        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("generates valid refresh JWT with three parts")
    void generateRefreshToken_returnsValidToken() {
        String token = jwtTokenProvider.generateRefreshToken(1L, "test-jti");

        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("returns userId from valid access token via validateAccessToken")
    void validateAccessToken_withValidAccessToken_returnsUserId() {
        String token = jwtTokenProvider.generateAccessToken(42L, "user42", "u42@example.com");

        Optional<Long> userId = jwtTokenProvider.validateAccessToken(token);

        assertThat(userId).hasValue(42L);
    }

    @Test
    @DisplayName("returns RefreshTokenClaims from valid refresh token via validateRefreshToken")
    void validateRefreshToken_withValidRefreshToken_returnsClaims() {
        String token = jwtTokenProvider.generateRefreshToken(42L, "jti-123");

        Optional<JwtTokenProvider.RefreshTokenClaims> claims = jwtTokenProvider.validateRefreshToken(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().userId()).isEqualTo(42L);
        assertThat(claims.get().jti()).isEqualTo("jti-123");
        assertThat(claims.get().expiresAt()).isAfter(java.time.Instant.now());
    }

    @Test
    @DisplayName("validateRefreshToken rejects access tokens")
    void validateRefreshToken_withAccessToken_returnsEmpty() {
        String token = jwtTokenProvider.generateAccessToken(1L, "testuser", "test@example.com");

        Optional<JwtTokenProvider.RefreshTokenClaims> result = jwtTokenProvider.validateRefreshToken(token);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("validateAccessToken rejects refresh tokens")
    void validateAccessToken_withRefreshToken_returnsEmpty() {
        String token = jwtTokenProvider.generateRefreshToken(1L, "jti-000");

        Optional<Long> userId = jwtTokenProvider.validateAccessToken(token);

        assertThat(userId).isEmpty();
    }

    @Test
    @DisplayName("returns correct userIds for multiple tokens")
    void validateAccessToken_withDifferentUserIds_returnsCorrectIds() {
        String token1 = jwtTokenProvider.generateAccessToken(1L, "user1", "u1@example.com");
        String token2 = jwtTokenProvider.generateAccessToken(100L, "user100", "u100@example.com");

        assertThat(jwtTokenProvider.validateAccessToken(token1)).hasValue(1L);
        assertThat(jwtTokenProvider.validateAccessToken(token2)).hasValue(100L);
    }

    @Test
    @DisplayName("returns empty for completely invalid token")
    void validateAccessToken_withInvalidToken_returnsEmpty() {
        Optional<Long> userId = jwtTokenProvider.validateAccessToken("invalid-token");

        assertThat(userId).isEmpty();
    }

    @Test
    @DisplayName("returns empty for tampered token")
    void validateAccessToken_withTamperedToken_returnsEmpty() {
        String token = jwtTokenProvider.generateAccessToken(1L, "testuser", "test@example.com");
        String tampered = token.substring(0, token.length() - 4) + "xxxx";

        Optional<Long> userId = jwtTokenProvider.validateAccessToken(tampered);

        assertThat(userId).isEmpty();
    }

    @Test
    @DisplayName("returns empty for null token")
    void validateAccessToken_withNullToken_returnsEmpty() {
        Optional<Long> userId = jwtTokenProvider.validateAccessToken(null);

        assertThat(userId).isEmpty();
    }

    @Test
    @DisplayName("returns empty for empty string token")
    void validateAccessToken_withEmptyString_returnsEmpty() {
        Optional<Long> userId = jwtTokenProvider.validateAccessToken("");

        assertThat(userId).isEmpty();
    }

    @Test
    @DisplayName("returns empty for expired access token")
    void validateAccessToken_withExpiredAccessToken_returnsEmpty() throws InterruptedException {
        JwtProperties shortLived = new JwtProperties(TEST_SECRET, 1, REFRESH_EXPIRATION_MS, ISSUER, AUDIENCE);
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(shortLived);
        String token = shortLivedProvider.generateAccessToken(1L, "testuser", "test@example.com");

        Thread.sleep(5);

        Optional<Long> userId = shortLivedProvider.validateAccessToken(token);

        assertThat(userId).isEmpty();
    }

    @Test
    @DisplayName("returns empty for expired refresh token")
    void validateRefreshToken_withExpiredRefreshToken_returnsEmpty() throws InterruptedException {
        JwtProperties shortLived = new JwtProperties(TEST_SECRET, EXPIRATION_MS, 1, ISSUER, AUDIENCE);
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(shortLived);
        String token = shortLivedProvider.generateRefreshToken(1L, "expired-jti");

        Thread.sleep(5);

        Optional<JwtTokenProvider.RefreshTokenClaims> result = shortLivedProvider.validateRefreshToken(token);

        assertThat(result).isEmpty();
    }
}

package com.morsel.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.morsel.config.JwtProperties;
import com.morsel.security.JwtTokenProvider.AccessTokenClaims;
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
        String token = jwtTokenProvider.generateAccessToken(1L, "testuser", "USER", true, true);

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
    @DisplayName("returns AccessTokenClaims from valid access token via extractAccessTokenClaims")
    void extractAccessTokenClaims_withValidAccessToken_returnsClaims() {
        String token = jwtTokenProvider.generateAccessToken(42L, "user42", "ADMIN", true, false);

        Optional<AccessTokenClaims> result = jwtTokenProvider.extractAccessTokenClaims(token);

        assertThat(result).isPresent();
        AccessTokenClaims claims = result.get();
        assertThat(claims.userId()).isEqualTo(42L);
        assertThat(claims.username()).isEqualTo("user42");
        assertThat(claims.role()).isEqualTo("ADMIN");
        assertThat(claims.enabled()).isTrue();
        assertThat(claims.accountNonLocked()).isFalse();
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
        String token = jwtTokenProvider.generateAccessToken(1L, "testuser", "USER", true, true);

        Optional<JwtTokenProvider.RefreshTokenClaims> result = jwtTokenProvider.validateRefreshToken(token);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("extractAccessTokenClaims rejects refresh tokens")
    void extractAccessTokenClaims_withRefreshToken_returnsEmpty() {
        String token = jwtTokenProvider.generateRefreshToken(1L, "jti-000");

        Optional<AccessTokenClaims> result = jwtTokenProvider.extractAccessTokenClaims(token);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns correct claims for multiple tokens")
    void extractAccessTokenClaims_withDifferentUserIds_returnsCorrectClaims() {
        String token1 = jwtTokenProvider.generateAccessToken(1L, "user1", "USER", true, true);
        String token2 = jwtTokenProvider.generateAccessToken(100L, "user100", "ADMIN", false, true);

        assertThat(jwtTokenProvider.extractAccessTokenClaims(token1))
                .isPresent()
                .hasValueSatisfying(claims -> {
                    assertThat(claims.userId()).isEqualTo(1L);
                    assertThat(claims.username()).isEqualTo("user1");
                    assertThat(claims.role()).isEqualTo("USER");
                    assertThat(claims.enabled()).isTrue();
                });

        assertThat(jwtTokenProvider.extractAccessTokenClaims(token2))
                .isPresent()
                .hasValueSatisfying(claims -> {
                    assertThat(claims.userId()).isEqualTo(100L);
                    assertThat(claims.username()).isEqualTo("user100");
                    assertThat(claims.role()).isEqualTo("ADMIN");
                    assertThat(claims.enabled()).isFalse();
                });
    }

    @Test
    @DisplayName("returns empty for completely invalid token")
    void extractAccessTokenClaims_withInvalidToken_returnsEmpty() {
        Optional<AccessTokenClaims> result = jwtTokenProvider.extractAccessTokenClaims("invalid-token");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns empty for tampered token")
    void extractAccessTokenClaims_withTamperedToken_returnsEmpty() {
        String token = jwtTokenProvider.generateAccessToken(1L, "testuser", "USER", true, true);
        String tampered = token.substring(0, token.length() - 4) + "xxxx";

        Optional<AccessTokenClaims> result = jwtTokenProvider.extractAccessTokenClaims(tampered);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns empty for null token")
    void extractAccessTokenClaims_withNullToken_returnsEmpty() {
        Optional<AccessTokenClaims> result = jwtTokenProvider.extractAccessTokenClaims(null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns empty for empty string token")
    void extractAccessTokenClaims_withEmptyString_returnsEmpty() {
        Optional<AccessTokenClaims> result = jwtTokenProvider.extractAccessTokenClaims("");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns empty for expired access token")
    void extractAccessTokenClaims_withExpiredAccessToken_returnsEmpty() throws InterruptedException {
        JwtProperties shortLived = new JwtProperties(TEST_SECRET, 1, REFRESH_EXPIRATION_MS, ISSUER, AUDIENCE);
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(shortLived);
        String token = shortLivedProvider.generateAccessToken(1L, "testuser", "USER", true, true);

        Thread.sleep(5);

        Optional<AccessTokenClaims> result = shortLivedProvider.extractAccessTokenClaims(token);

        assertThat(result).isEmpty();
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

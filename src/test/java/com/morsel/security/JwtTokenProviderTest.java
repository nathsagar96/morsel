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

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties(TEST_SECRET, EXPIRATION_MS);
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    @Test
    @DisplayName("generates valid JWT with three parts")
    void generateToken_returnsValidToken() {
        String token = jwtTokenProvider.generateToken(1L);

        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("returns userId from valid token")
    void getUserIdIfValid_withValidToken_returnsUserId() {
        String token = jwtTokenProvider.generateToken(42L);

        Optional<Long> userId = jwtTokenProvider.getUserIdIfValid(token);

        assertThat(userId).hasValue(42L);
    }

    @Test
    @DisplayName("returns correct userIds for multiple tokens")
    void getUserIdIfValid_withDifferentUserIds_returnsCorrectIds() {
        String token1 = jwtTokenProvider.generateToken(1L);
        String token2 = jwtTokenProvider.generateToken(100L);

        assertThat(jwtTokenProvider.getUserIdIfValid(token1)).hasValue(1L);
        assertThat(jwtTokenProvider.getUserIdIfValid(token2)).hasValue(100L);
    }

    @Test
    @DisplayName("returns empty for completely invalid token")
    void getUserIdIfValid_withInvalidToken_returnsEmpty() {
        Optional<Long> userId = jwtTokenProvider.getUserIdIfValid("invalid-token");

        assertThat(userId).isEmpty();
    }

    @Test
    @DisplayName("returns empty for tampered token")
    void getUserIdIfValid_withTamperedToken_returnsEmpty() {
        String token = jwtTokenProvider.generateToken(1L);
        String tampered = token.substring(0, token.length() - 4) + "xxxx";

        Optional<Long> userId = jwtTokenProvider.getUserIdIfValid(tampered);

        assertThat(userId).isEmpty();
    }

    @Test
    @DisplayName("returns empty for null token")
    void getUserIdIfValid_withNullToken_returnsEmpty() {
        Optional<Long> userId = jwtTokenProvider.getUserIdIfValid(null);

        assertThat(userId).isEmpty();
    }

    @Test
    @DisplayName("returns empty for empty string token")
    void getUserIdIfValid_withEmptyString_returnsEmpty() {
        Optional<Long> userId = jwtTokenProvider.getUserIdIfValid("");

        assertThat(userId).isEmpty();
    }

    @Test
    @DisplayName("returns empty for expired token")
    void getUserIdIfValid_withExpiredToken_returnsEmpty() throws InterruptedException {
        JwtProperties shortLived = new JwtProperties(TEST_SECRET, 1);
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(shortLived);
        String token = shortLivedProvider.generateToken(1L);

        Thread.sleep(5);

        Optional<Long> userId = shortLivedProvider.getUserIdIfValid(token);

        assertThat(userId).isEmpty();
    }
}

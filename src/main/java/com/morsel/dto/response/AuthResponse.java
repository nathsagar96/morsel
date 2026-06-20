package com.morsel.dto.response;

import com.morsel.constants.AuthConstants;
import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature")
        String token,

        @Schema(example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature_refresh")
        String refreshToken,

        @Schema(example = "Bearer") String type,
        @Schema(example = "1") Long id,
        @Schema(example = "janedoe") String username,
        @Schema(example = "jane.doe@example.com") String email) {

    public static AuthResponse of(String token, String refreshToken, Long id, String username, String email) {
        return new AuthResponse(token, refreshToken, AuthConstants.BEARER_TYPE, id, username, email);
    }
}

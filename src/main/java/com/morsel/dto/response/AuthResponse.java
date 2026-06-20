package com.morsel.dto.response;

import com.morsel.constants.AuthConstants;

public record AuthResponse(String token, String refreshToken, String type, Long id, String username, String email) {

    public static AuthResponse of(String token, String refreshToken, Long id, String username, String email) {
        return new AuthResponse(token, refreshToken, AuthConstants.BEARER_TYPE, id, username, email);
    }
}

package com.morsel.dto.response;

public record AuthResponse(String token, String type, Long id, String username, String email) {

    public static AuthResponse of(String token, Long id, String username, String email) {
        return new AuthResponse(token, "Bearer", id, username, email);
    }
}

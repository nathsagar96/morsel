package com.morsel.mapper;

import com.morsel.dto.request.SignUpRequest;
import com.morsel.dto.response.AuthResponse;
import com.morsel.dto.response.UserProfileResponse;
import com.morsel.model.Role;
import com.morsel.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(SignUpRequest request, String encodedPassword, Role role) {
        return User.builder()
                .username(request.username().trim().toLowerCase())
                .email(request.email().trim().toLowerCase())
                .password(encodedPassword)
                .role(role)
                .build();
    }

    public AuthResponse toAuthResponse(User user, String token, String refreshToken) {
        return AuthResponse.of(token, refreshToken, user.getId(), user.getUsername(), user.getEmail());
    }

    public UserProfileResponse toProfileResponse(User user, int recipeCount) {
        return UserProfileResponse.of(user, recipeCount);
    }
}

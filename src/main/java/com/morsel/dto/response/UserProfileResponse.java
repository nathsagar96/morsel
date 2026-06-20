package com.morsel.dto.response;

import com.morsel.model.User;

public record UserProfileResponse(Long id, String username, int recipeCount) {

    public static UserProfileResponse of(User user, int recipeCount) {
        return new UserProfileResponse(user.getId(), user.getUsername(), recipeCount);
    }
}

package com.morsel.dto.response;

import com.morsel.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserProfileResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "janedoe") String username,
        @Schema(example = "12") int recipeCount) {

    public static UserProfileResponse of(User user, int recipeCount) {
        return new UserProfileResponse(user.getId(), user.getUsername(), recipeCount);
    }
}

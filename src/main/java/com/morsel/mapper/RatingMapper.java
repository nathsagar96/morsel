package com.morsel.mapper;

import com.morsel.dto.request.RatingRequest;
import com.morsel.dto.response.RatingResponse;
import com.morsel.model.Rating;
import com.morsel.model.Recipe;
import com.morsel.model.User;
import org.springframework.stereotype.Component;

@Component
public class RatingMapper {

    public Rating toEntity(RatingRequest request, Recipe recipe, User user) {
        return Rating.builder().score(request.score()).recipe(recipe).user(user).build();
    }

    public RatingResponse toResponse(Rating rating) {
        return RatingResponse.of(rating);
    }
}

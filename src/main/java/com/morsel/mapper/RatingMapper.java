package com.morsel.mapper;

import com.morsel.dto.response.RatingResponse;
import com.morsel.model.Rating;
import org.springframework.stereotype.Component;

@Component
public class RatingMapper {

    public RatingResponse toResponse(Rating rating) {
        return RatingResponse.of(rating);
    }
}

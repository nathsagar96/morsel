package com.morsel.controller;

import com.morsel.constants.ApiPaths;
import com.morsel.dto.request.RatingRequest;
import com.morsel.dto.response.RatingResponse;
import com.morsel.security.UserPrincipal;
import com.morsel.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.RECIPE_RATINGS)
@RequiredArgsConstructor
@Slf4j
public class RatingController {

    private final RatingService ratingService;

    @PutMapping("/me")
    public RatingResponse addOrUpdateRating(
            @PathVariable Long recipeId,
            @Valid @RequestBody RatingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug(
                "Add/update rating for recipe id={} by user={}",
                recipeId,
                principal.user().getId());
        return ratingService.addOrUpdateRating(recipeId, request, principal.user());
    }
}

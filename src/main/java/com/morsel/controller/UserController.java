package com.morsel.controller;

import com.morsel.dto.response.RecipeResponse;
import com.morsel.dto.response.UserProfileResponse;
import com.morsel.security.UserPrincipal;
import com.morsel.service.FavoriteService;
import com.morsel.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserProfileService userProfileService;
    private final FavoriteService favoriteService;

    @GetMapping("/{username}")
    public UserProfileResponse getProfile(@PathVariable String username) {
        log.debug("Profile request for user: {}", username);
        return userProfileService.getProfile(username);
    }

    @GetMapping("/me/favorites")
    public Page<RecipeResponse> getMyFavorites(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("Favorites request by user: {}", principal.user().getId());
        return favoriteService.getFavorites(principal.user(), pageable);
    }
}

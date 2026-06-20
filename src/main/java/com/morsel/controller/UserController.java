package com.morsel.controller;

import com.morsel.constants.ApiPaths;
import com.morsel.constants.ErrorMessages;
import com.morsel.dto.request.UserStatusRequest;
import com.morsel.dto.response.RecipeResponse;
import com.morsel.dto.response.UserProfileResponse;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.logging.AuditLogger;
import com.morsel.logging.AuditLogger.Event;
import com.morsel.logging.AuditLogger.Outcome;
import com.morsel.logging.PiiSanitizer;
import com.morsel.model.User;
import com.morsel.repository.UserRepository;
import com.morsel.security.UserPrincipal;
import com.morsel.service.FavoriteService;
import com.morsel.service.UserProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.USERS)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User profiles and account management")
public class UserController {

    private final UserProfileService userProfileService;
    private final FavoriteService favoriteService;
    private final UserRepository userRepository;

    @GetMapping("/{username}")
    public UserProfileResponse getProfile(@PathVariable String username) {
        log.debug("Profile request for user: {}", PiiSanitizer.sanitizeIdentifier(username));
        return userProfileService.getProfile(username);
    }

    @GetMapping("/me/favorites")
    public Page<RecipeResponse> getMyFavorites(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("Favorites request by user: {}", principal.user().getId());
        return favoriteService.getFavorites(principal.user(), pageable);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> updateUserStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        log.debug("Admin request to update user {} status, enabled={}", id, request.enabled());
        User user =
                userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setEnabled(request.enabled());
        userRepository.save(user);
        log.info("User {} {} by admin", id, request.enabled() ? "enabled" : "disabled");
        AuditLogger.log(Event.ADMIN_USER_STATUS_CHANGE, id, Outcome.SUCCESS, "enabled=" + request.enabled());
        return Map.of(
                ErrorMessages.MESSAGE_KEY, "User " + (request.enabled() ? "enabled" : "disabled") + " successfully");
    }
}

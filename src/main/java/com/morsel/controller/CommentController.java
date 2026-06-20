package com.morsel.controller;

import com.morsel.constants.ApiPaths;
import com.morsel.dto.request.CommentRequest;
import com.morsel.dto.response.CommentResponse;
import com.morsel.security.UserPrincipal;
import com.morsel.service.CommentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.RECIPE_COMMENTS)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Comments", description = "Add and list comments on recipes")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(
            @PathVariable Long recipeId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug(
                "Add comment to recipe id={} by user={}",
                recipeId,
                principal.user().getId());
        return commentService.addComment(recipeId, request, principal.user());
    }

    @GetMapping
    @SecurityRequirements
    public Page<CommentResponse> getComments(
            @PathVariable Long recipeId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("List comments for recipe id={}, pageable={}", recipeId, pageable);
        return commentService.getComments(recipeId, pageable);
    }
}

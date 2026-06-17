package com.morsel.controller;

import com.morsel.dto.request.CommentRequest;
import com.morsel.dto.response.CommentResponse;
import com.morsel.security.UserPrincipal;
import com.morsel.service.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/api/v1/recipes/{recipeId}/comments")
@RequiredArgsConstructor
@Slf4j
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
    public List<CommentResponse> getComments(@PathVariable Long recipeId) {
        log.debug("List comments for recipe id={}", recipeId);
        return commentService.getComments(recipeId);
    }
}

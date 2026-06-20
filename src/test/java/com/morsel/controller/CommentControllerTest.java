package com.morsel.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.morsel.dto.request.CommentRequest;
import com.morsel.dto.response.CommentResponse;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.model.Role;
import com.morsel.model.User;
import com.morsel.repository.UserRepository;
import com.morsel.security.JwtTokenProvider;
import com.morsel.security.UserPrincipal;
import com.morsel.service.CommentService;
import com.morsel.service.CustomUserDetailsService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = CommentController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CommentController")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    private final User user =
            User.builder().id(1L).username("user").role(Role.USER).build();
    private final UserPrincipal userPrincipal = new UserPrincipal(user);
    private final Instant now = Instant.now();
    private final CommentResponse commentResponse = new CommentResponse(10L, "Great recipe!", 1L, "user", 100L, now);

    @BeforeEach
    void setUpAuth() {
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{recipeId}/comments returns 201")
    void addComment_withValidRequest_returns201() throws Exception {
        when(commentService.addComment(eq(100L), any(CommentRequest.class), any(User.class)))
                .thenReturn(commentResponse);

        mockMvc.perform(post("/api/v1/recipes/100/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"Great recipe!"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.text").value("Great recipe!"))
                .andExpect(jsonPath("$.username").value("user"));
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{recipeId}/comments returns 400 for blank text")
    void addComment_withBlankText_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/100/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{recipeId}/comments returns 404 when recipe not found")
    void addComment_withNonExistentRecipe_returns404() throws Exception {
        when(commentService.addComment(eq(999L), any(CommentRequest.class), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Recipe not found with id: 999"));

        mockMvc.perform(post("/api/v1/recipes/999/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"Great recipe!"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/recipes/{recipeId}/comments returns paginated list")
    void getComments_withExistingRecipe_returns200() throws Exception {
        when(commentService.getComments(eq(100L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(commentResponse)));

        mockMvc.perform(get("/api/v1/recipes/100/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].text").value("Great recipe!"));
    }

    @Test
    @DisplayName("GET /api/v1/recipes/{recipeId}/comments returns 404 when recipe not found")
    void getComments_withNonExistentRecipe_returns404() throws Exception {
        when(commentService.getComments(eq(999L), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException("Recipe not found with id: 999"));

        mockMvc.perform(get("/api/v1/recipes/999/comments")).andExpect(status().isNotFound());
    }
}

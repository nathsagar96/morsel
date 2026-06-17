package com.morsel.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.morsel.dto.request.RatingRequest;
import com.morsel.dto.response.RatingResponse;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.model.Role;
import com.morsel.model.User;
import com.morsel.security.JwtTokenProvider;
import com.morsel.security.UserPrincipal;
import com.morsel.service.CustomUserDetailsService;
import com.morsel.service.RatingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = RatingController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("RatingController")
class RatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RatingService ratingService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private final User user =
            User.builder().id(1L).username("user").role(Role.USER).build();
    private final UserPrincipal userPrincipal = new UserPrincipal(user);
    private final RatingResponse ratingResponse = new RatingResponse(10L, 4, 1L, "user", 100L);

    @BeforeEach
    void setUpAuth() {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{recipeId}/ratings returns 201")
    void addOrUpdateRating_withValidRequest_returns201() throws Exception {
        when(ratingService.addOrUpdateRating(eq(100L), any(RatingRequest.class), any(User.class)))
                .thenReturn(ratingResponse);

        mockMvc.perform(post("/api/v1/recipes/100/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":4}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.score").value(4))
                .andExpect(jsonPath("$.username").value("user"));
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{recipeId}/ratings returns 400 for score below 1")
    void addOrUpdateRating_withScoreBelowMin_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/100/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{recipeId}/ratings returns 400 for score above 5")
    void addOrUpdateRating_withScoreAboveMax_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/100/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":6}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{recipeId}/ratings returns 404 when recipe not found")
    void addOrUpdateRating_withNonExistentRecipe_returns404() throws Exception {
        when(ratingService.addOrUpdateRating(eq(999L), any(RatingRequest.class), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Recipe not found with id: 999"));

        mockMvc.perform(post("/api/v1/recipes/999/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":4}
                                """))
                .andExpect(status().isNotFound());
    }
}

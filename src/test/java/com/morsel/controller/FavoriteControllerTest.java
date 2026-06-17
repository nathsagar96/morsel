package com.morsel.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.morsel.exception.DuplicateResourceException;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.model.Role;
import com.morsel.model.User;
import com.morsel.security.JwtTokenProvider;
import com.morsel.security.UserPrincipal;
import com.morsel.service.CustomUserDetailsService;
import com.morsel.service.FavoriteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = FavoriteController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FavoriteController")
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FavoriteService favoriteService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private final User user =
            User.builder().id(1L).username("testuser").role(Role.USER).build();
    private final UserPrincipal userPrincipal = new UserPrincipal(user);

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
    @DisplayName("POST /api/v1/recipes/{id}/favorite returns 201")
    void addFavorite_withValidIds_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/100/favorite")).andExpect(status().isCreated());

        verify(favoriteService).favorite(eq(100L), any(User.class));
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{id}/favorite returns 409 when already favorited")
    void addFavorite_withDuplicate_returns409() throws Exception {
        doThrow(new DuplicateResourceException("Recipe already in favorites"))
                .when(favoriteService)
                .favorite(eq(100L), any(User.class));

        mockMvc.perform(post("/api/v1/recipes/100/favorite")).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{id}/favorite returns 404 when recipe not found")
    void addFavorite_withNonExistentRecipe_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Recipe not found with id: 999"))
                .when(favoriteService)
                .favorite(eq(999L), any(User.class));

        mockMvc.perform(post("/api/v1/recipes/999/favorite")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/recipes/{id}/favorite returns 204")
    void removeFavorite_withExistingFavorite_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/recipes/100/favorite")).andExpect(status().isNoContent());

        verify(favoriteService).unfavorite(eq(100L), any(User.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/recipes/{id}/favorite returns 404 when not favorited")
    void removeFavorite_withNonFavorited_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Recipe not in favorites"))
                .when(favoriteService)
                .unfavorite(eq(999L), any(User.class));

        mockMvc.perform(delete("/api/v1/recipes/999/favorite")).andExpect(status().isNotFound());
    }
}

package com.morsel.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.morsel.dto.response.UserProfileResponse;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.model.Role;
import com.morsel.model.User;
import com.morsel.security.JwtTokenProvider;
import com.morsel.security.UserPrincipal;
import com.morsel.service.CustomUserDetailsService;
import com.morsel.service.FavoriteService;
import com.morsel.service.UserService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = UserController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private FavoriteService favoriteService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private final User user =
            User.builder().id(1L).username("testuser").role(Role.USER).build();
    private final UserPrincipal userPrincipal = new UserPrincipal(user);
    private final UserProfileResponse profileResponse = new UserProfileResponse(1L, "testuser", List.of());

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
    @DisplayName("GET /api/v1/users/{username} returns profile when found")
    void getProfile_withExistingUsername_returns200() throws Exception {
        when(userService.getProfile("testuser")).thenReturn(profileResponse);

        mockMvc.perform(get("/api/v1/users/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{username} returns 404 when not found")
    void getProfile_withNonExistentUser_returns404() throws Exception {
        when(userService.getProfile("unknown")).thenThrow(new ResourceNotFoundException("User not found: unknown"));

        mockMvc.perform(get("/api/v1/users/unknown")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/users/me/favorites returns list")
    void getMyFavorites_returns200() throws Exception {
        when(favoriteService.getFavorites(any(User.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users/me/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}

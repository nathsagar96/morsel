package com.morsel.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.morsel.dto.request.IngredientRequest;
import com.morsel.dto.response.IngredientResponse;
import com.morsel.exception.DuplicateResourceException;
import com.morsel.exception.ForbiddenException;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.model.Role;
import com.morsel.model.User;
import com.morsel.repository.UserRepository;
import com.morsel.security.JwtTokenProvider;
import com.morsel.security.UserPrincipal;
import com.morsel.service.CustomUserDetailsService;
import com.morsel.service.IngredientService;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = IngredientController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("IngredientController")
class IngredientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IngredientService ingredientService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    private final User admin =
            User.builder().id(2L).username("admin").role(Role.ADMIN).build();
    private final UserPrincipal adminPrincipal = new UserPrincipal(admin);
    private final Instant now = Instant.now();
    private final IngredientResponse ingredientResponse = new IngredientResponse(10L, "Tomato", now, now);

    @BeforeEach
    void setUpAuth() {
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/ingredients returns paginated list")
    void findAll_returns200WithPage() throws Exception {
        when(ingredientService.findAll(any(), any())).thenReturn(new PageImpl<>(List.of(ingredientResponse)));

        mockMvc.perform(get("/api/v1/ingredients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].name").value("Tomato"));
    }

    @Test
    @DisplayName("GET /api/v1/ingredients filters by keyword")
    void findAll_withKeyword_returnsFilteredResults() throws Exception {
        when(ingredientService.findAll(eq("Tom"), any())).thenReturn(new PageImpl<>(List.of(ingredientResponse)));

        mockMvc.perform(get("/api/v1/ingredients?keyword=Tom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Tomato"));
    }

    @Test
    @DisplayName("GET /api/v1/ingredients/{id} returns ingredient when found")
    void findById_withExistingId_returns200() throws Exception {
        when(ingredientService.findById(10L)).thenReturn(ingredientResponse);

        mockMvc.perform(get("/api/v1/ingredients/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Tomato"));
    }

    @Test
    @DisplayName("GET /api/v1/ingredients/{id} returns 404 when not found")
    void findById_withNonExistentId_returns404() throws Exception {
        when(ingredientService.findById(999L))
                .thenThrow(new ResourceNotFoundException("Ingredient not found with id: 999"));

        mockMvc.perform(get("/api/v1/ingredients/999")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/ingredients returns 201 for valid request")
    void create_withValidRequest_returns201() throws Exception {
        when(ingredientService.create(any(IngredientRequest.class))).thenReturn(ingredientResponse);

        mockMvc.perform(post("/api/v1/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Tomato"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Tomato"));
    }

    @Test
    @DisplayName("POST /api/v1/ingredients returns 400 for missing name")
    void create_withMissingName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/ingredients returns 409 for duplicate name")
    void create_withDuplicateName_returns409() throws Exception {
        when(ingredientService.create(any(IngredientRequest.class)))
                .thenThrow(new DuplicateResourceException("Ingredient with name already exists: Tomato"));

        mockMvc.perform(post("/api/v1/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Tomato"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /api/v1/ingredients/{id} returns 200 for valid update")
    void update_withValidRequest_returns200() throws Exception {
        when(ingredientService.update(eq(10L), any(IngredientRequest.class))).thenReturn(ingredientResponse);

        mockMvc.perform(put("/api/v1/ingredients/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Tomato"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("PUT /api/v1/ingredients/{id} returns 404 when not found")
    void update_withNonExistentId_returns404() throws Exception {
        when(ingredientService.update(eq(999L), any(IngredientRequest.class)))
                .thenThrow(new ResourceNotFoundException("Ingredient not found with id: 999"));

        mockMvc.perform(put("/api/v1/ingredients/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"NewName"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/ingredients/{id} returns 204 when admin deletes")
    void delete_byAdmin_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/ingredients/10")).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/ingredients/{id} returns 403 when non-admin deletes")
    void delete_byNonAdmin_returns403() throws Exception {
        User regularUser =
                User.builder().id(1L).username("user").role(Role.USER).build();
        UserPrincipal userPrincipal = new UserPrincipal(regularUser);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities()));

        doThrow(new ForbiddenException("Only admins can delete ingredients"))
                .when(ingredientService)
                .delete(eq(10L), any(User.class));

        mockMvc.perform(delete("/api/v1/ingredients/10")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/ingredients/{id} returns 404 when not found")
    void delete_withNonExistentId_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Ingredient not found with id: 999"))
                .when(ingredientService)
                .delete(eq(999L), any(User.class));

        mockMvc.perform(delete("/api/v1/ingredients/999")).andExpect(status().isNotFound());
    }
}

package com.morsel.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.morsel.dto.request.CreateRecipeRequest;
import com.morsel.dto.request.UpdateRecipeRequest;
import com.morsel.dto.response.RecipeResponse;
import com.morsel.dto.response.RecipeSummaryResponse;
import com.morsel.exception.ForbiddenException;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.model.Role;
import com.morsel.model.User;
import com.morsel.repository.UserRepository;
import com.morsel.security.JwtTokenProvider;
import com.morsel.security.UserPrincipal;
import com.morsel.service.CustomUserDetailsService;
import com.morsel.service.RecipeService;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = RecipeController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("RecipeController")
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeService recipeService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    private final User author =
            User.builder().id(1L).username("author").role(Role.USER).build();
    private final User admin =
            User.builder().id(2L).username("admin").role(Role.ADMIN).build();
    private final UserPrincipal authorPrincipal = new UserPrincipal(author);
    private final UserPrincipal adminPrincipal = new UserPrincipal(admin);
    private final Instant now = Instant.now();
    private final RecipeResponse recipeResponse =
            new RecipeResponse(100L, "Title", "Desc", "Steps", null, 1L, "author", List.of(10L), 0.0, 0, now, now);
    private final RecipeSummaryResponse recipeSummaryResponse =
            new RecipeSummaryResponse(100L, "Title", "Desc", null, 1L, "author", List.of(10L), 0.0, 0, now, now);

    @BeforeEach
    void setUpAuth() {
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        authorPrincipal, null, authorPrincipal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/v1/recipes returns 201 with recipe for valid request")
    void create_withValidRequest_returns201() throws Exception {
        when(recipeService.create(any(CreateRecipeRequest.class), any(User.class)))
                .thenReturn(recipeResponse);

        mockMvc.perform(post("/api/v1/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Title","description":"Desc","instructions":"Steps","ingredientIds":[10]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.title").value("Title"))
                .andExpect(jsonPath("$.authorUsername").value("author"));
    }

    @Test
    @DisplayName("POST /api/v1/recipes returns 400 for missing title")
    void create_withMissingTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instructions":"Steps"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/recipes returns 400 for missing instructions")
    void create_withMissingInstructions_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Title"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/recipes returns paginated list")
    void findAll_returns200WithPage() throws Exception {
        when(recipeService.findAll(any(), any(), any())).thenReturn(new PageImpl<>(List.of(recipeSummaryResponse)));

        mockMvc.perform(get("/api/v1/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.content[0].title").value("Title"));
    }

    @Test
    @DisplayName("GET /api/v1/recipes filters by keyword")
    void findAll_withKeyword_returnsFilteredResults() throws Exception {
        when(recipeService.findAll(eq("pasta"), any(), any()))
                .thenReturn(new PageImpl<>(List.of(recipeSummaryResponse)));

        mockMvc.perform(get("/api/v1/recipes?keyword=pasta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100));
    }

    @Test
    @DisplayName("GET /api/v1/recipes filters by ingredients")
    void findAll_withIngredients_returnsFilteredResults() throws Exception {
        when(recipeService.findAll(any(), eq(List.of(1L, 2L)), any()))
                .thenReturn(new PageImpl<>(List.of(recipeSummaryResponse)));

        mockMvc.perform(get("/api/v1/recipes?ingredients=1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100));
    }

    @Test
    @DisplayName("GET /api/v1/recipes filters by keyword and ingredients combined")
    void findAll_withKeywordAndIngredients_returnsCombinedResults() throws Exception {
        when(recipeService.findAll(eq("pasta"), eq(List.of(1L, 2L)), any()))
                .thenReturn(new PageImpl<>(List.of(recipeSummaryResponse)));

        mockMvc.perform(get("/api/v1/recipes?keyword=pasta&ingredients=1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100));
    }

    @Test
    @DisplayName("GET /api/v1/recipes returns 400 for invalid sort field")
    void findAll_withInvalidSortField_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/recipes?sort=invalidField,asc")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/recipes/{id} returns recipe when found")
    void findById_withExistingId_returns200() throws Exception {
        when(recipeService.findById(100L)).thenReturn(recipeResponse);

        mockMvc.perform(get("/api/v1/recipes/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    @DisplayName("GET /api/v1/recipes/{id} returns 404 when not found")
    void findById_withNonExistentId_returns404() throws Exception {
        when(recipeService.findById(999L)).thenThrow(new ResourceNotFoundException("Recipe not found with id: 999"));

        mockMvc.perform(get("/api/v1/recipes/999")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/recipes/{id} returns 200 when owner updates")
    void update_byOwner_returns200() throws Exception {
        when(recipeService.update(eq(100L), any(UpdateRecipeRequest.class), any(User.class)))
                .thenReturn(recipeResponse);

        mockMvc.perform(put("/api/v1/recipes/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Title","instructions":"Steps","ingredientIds":[1,2]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));
    }

    @Test
    @DisplayName("PUT /api/v1/recipes/{id} returns 403 when non-owner updates")
    void update_byNonOwner_returns403() throws Exception {
        when(recipeService.update(eq(100L), any(UpdateRecipeRequest.class), any(User.class)))
                .thenThrow(new ForbiddenException("You do not own this recipe"));

        mockMvc.perform(put("/api/v1/recipes/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Title","instructions":"Steps","ingredientIds":[1,2]}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/recipes/{id} returns 404 when not found")
    void update_withNonExistentId_returns404() throws Exception {
        when(recipeService.update(eq(999L), any(UpdateRecipeRequest.class), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Recipe not found with id: 999"));

        mockMvc.perform(put("/api/v1/recipes/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Title","instructions":"Steps","ingredientIds":[1,2]}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/recipes/{id} returns 400 for missing title")
    void update_withMissingTitle_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/recipes/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instructions":"Steps","ingredientIds":[1,2]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/v1/recipes/{id} returns 204 when admin deletes")
    void delete_byAdmin_returns204() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities()));

        mockMvc.perform(delete("/api/v1/recipes/100")).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/recipes/{id} returns 403 when non-admin deletes")
    void delete_byNonAdmin_returns403() throws Exception {
        doThrow(new ForbiddenException("Only admins can delete recipes"))
                .when(recipeService)
                .delete(eq(100L), any(User.class));

        mockMvc.perform(delete("/api/v1/recipes/100")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/recipes/{id} returns 404 when not found")
    void delete_withNonExistentId_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Recipe not found with id: 999"))
                .when(recipeService)
                .delete(eq(999L), any(User.class));

        mockMvc.perform(delete("/api/v1/recipes/999")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{id}/image returns 201 for owner with valid file")
    void uploadImage_byOwner_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "image-content".getBytes());
        when(recipeService.uploadImage(eq(100L), any(), any(User.class))).thenReturn(recipeResponse);

        mockMvc.perform(multipart(HttpMethod.POST, "/api/v1/recipes/100/image").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100));
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{id}/image returns 403 when non-owner uploads")
    void uploadImage_byNonOwner_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "image-content".getBytes());
        when(recipeService.uploadImage(eq(100L), any(), any(User.class)))
                .thenThrow(new ForbiddenException("You do not own this recipe"));

        mockMvc.perform(multipart(HttpMethod.POST, "/api/v1/recipes/100/image").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{id}/image returns 404 when recipe not found")
    void uploadImage_withNonExistentId_returns404() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "image-content".getBytes());
        when(recipeService.uploadImage(eq(999L), any(), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Recipe not found with id: 999"));

        mockMvc.perform(multipart(HttpMethod.POST, "/api/v1/recipes/999/image").file(file))
                .andExpect(status().isNotFound());
    }
}

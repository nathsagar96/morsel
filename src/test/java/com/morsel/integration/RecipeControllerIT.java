package com.morsel.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.morsel.model.Ingredient;
import com.morsel.model.Role;
import com.morsel.model.User;
import com.morsel.repository.IngredientRepository;
import com.morsel.repository.RecipeRepository;
import com.morsel.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

@DisplayName("RecipeController IT")
class RecipeControllerIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    private String userToken;
    private String secondUserToken;
    private String adminToken;
    private Long ingredientId;

    @BeforeEach
    void setUp() throws Exception {
        recipeRepository.deleteAll();
        userRepository.deleteAll();
        ingredientRepository.deleteAll();

        Ingredient tomato =
                ingredientRepository.save(Ingredient.builder().name("Tomato").build());
        ingredientId = tomato.getId();

        SignUpResult userResult = signUp("recipeuser", "user@example.com", "Password1!");
        userToken = userResult.token();

        SignUpResult secondUserResult = signUp("seconduser", "second@example.com", "Password1!");
        secondUserToken = secondUserResult.token();

        SignUpResult adminResult = signUp("recipeadmin", "admin@example.com", "Password1!");
        User admin = userRepository.findById(adminResult.userId()).orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        adminToken = signInAndGetToken("recipeadmin", "Password1!");
    }

    @Test
    @DisplayName("POST /api/v1/recipes returns 201 for authenticated user")
    void createRecipe_asAuthenticatedUser_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Pasta","description":"Italian dish","instructions":"Boil and serve","ingredientIds":[%d]}
                                """.formatted(ingredientId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Pasta"))
                .andExpect(jsonPath("$.authorUsername").value("recipeuser"));
    }

    @Test
    @DisplayName("POST /api/v1/recipes returns 401 without auth token")
    void createRecipe_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Pasta","instructions":"Boil and serve","ingredientIds":[%d]}
                                """.formatted(ingredientId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/recipes returns 400 for missing title")
    void createRecipe_withMissingTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instructions":"Boil and serve","ingredientIds":[%d]}
                                """.formatted(ingredientId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/recipes returns 200 with recipe list")
    void getAllRecipes_returns200WithPage() throws Exception {
        mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Soup","instructions":"Boil","ingredientIds":[%d]}
                                """.formatted(ingredientId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Soup"));
    }

    @Test
    @DisplayName("GET /api/v1/recipes/{id} returns 200 when recipe exists")
    void getRecipeById_whenExists_returns200() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Pasta","instructions":"Boil","ingredientIds":[%d]}
                                """.formatted(ingredientId)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long recipeId = ((Number) com.jayway.jsonpath.JsonPath.read(createResponse, "$.id")).longValue();

        mockMvc.perform(get("/api/v1/recipes/{id}", recipeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Pasta"));
    }

    @Test
    @DisplayName("GET /api/v1/recipes/{id} returns 404 when recipe not found")
    void getRecipeById_whenNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/recipes/999")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/recipes filters by keyword")
    void getRecipes_withKeyword_returnsFiltered() throws Exception {
        mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Chicken Soup","instructions":"Boil","ingredientIds":[%d]}
                                """.formatted(ingredientId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Beef Stew","instructions":"Braise","ingredientIds":[%d]}
                                """.formatted(ingredientId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/recipes?keyword=chicken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Chicken Soup"));
    }

    @Test
    @DisplayName("PUT /api/v1/recipes/{id} returns 200 for owner update")
    void updateRecipe_asOwner_returns200() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Old Title","instructions":"Steps","ingredientIds":[%d]}
                                """.formatted(ingredientId)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long recipeId = ((Number) com.jayway.jsonpath.JsonPath.read(createResponse, "$.id")).longValue();

        mockMvc.perform(put("/api/v1/recipes/{id}", recipeId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"New Title","instructions":"Updated steps","ingredientIds":[%d]}
                                """.formatted(ingredientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"));
    }

    @Test
    @DisplayName("PUT /api/v1/recipes/{id} returns 403 for non-owner update")
    void updateRecipe_asNonOwner_returns403() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"My Recipe","instructions":"Steps","ingredientIds":[%d]}
                                """.formatted(ingredientId)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long recipeId = ((Number) com.jayway.jsonpath.JsonPath.read(createResponse, "$.id")).longValue();

        mockMvc.perform(put("/api/v1/recipes/{id}", recipeId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Hijacked","instructions":"Steps","ingredientIds":[%d]}
                                """.formatted(ingredientId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/recipes/{id} returns 404 when recipe not found")
    void updateRecipe_whenNotFound_returns404() throws Exception {
        mockMvc.perform(put("/api/v1/recipes/999")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"X","instructions":"Y","ingredientIds":[%d]}
                                """.formatted(ingredientId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/recipes/{id} returns 204 for admin")
    void deleteRecipe_asAdmin_returns204() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"To Delete","instructions":"Steps","ingredientIds":[%d]}
                                """.formatted(ingredientId)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long recipeId = ((Number) com.jayway.jsonpath.JsonPath.read(createResponse, "$.id")).longValue();

        mockMvc.perform(delete("/api/v1/recipes/{id}", recipeId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/recipes/{id} returns 403 for non-owner")
    void deleteRecipe_asNonOwner_returns403() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Not Yours","instructions":"Steps","ingredientIds":[%d]}
                                """.formatted(ingredientId)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long recipeId = ((Number) com.jayway.jsonpath.JsonPath.read(createResponse, "$.id")).longValue();

        mockMvc.perform(delete("/api/v1/recipes/{id}", recipeId).header("Authorization", "Bearer " + secondUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/recipes/{id} returns 204 for owner")
    void deleteRecipe_asOwner_returns204() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"My Recipe","instructions":"Steps","ingredientIds":[%d]}
                                """.formatted(ingredientId)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long recipeId = ((Number) com.jayway.jsonpath.JsonPath.read(createResponse, "$.id")).longValue();

        mockMvc.perform(delete("/api/v1/recipes/{id}", recipeId).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/recipes/{id} returns 404 when recipe not found")
    void deleteRecipe_whenNotFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/recipes/999").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}

package com.morsel.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.morsel.model.Ingredient;
import com.morsel.repository.IngredientRepository;
import com.morsel.repository.RecipeRepository;
import com.morsel.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

@DisplayName("RatingController IT")
class RatingControllerIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    private String userToken;
    private Long recipeId;

    @BeforeEach
    void setUp() throws Exception {
        recipeRepository.deleteAll();
        userRepository.deleteAll();
        ingredientRepository.deleteAll();

        Ingredient ingredient =
                ingredientRepository.save(Ingredient.builder().name("Tomato").build());
        SignUpResult userResult = signUp("rater", "rate@example.com", "Password1!");
        userToken = userResult.token();

        String createResponse = mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Rateable Recipe","instructions":"Make it","ingredientIds":[%d]}
                                """.formatted(ingredient.getId())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        recipeId = ((Number) com.jayway.jsonpath.JsonPath.read(createResponse, "$.id")).longValue();
    }

    @Test
    @DisplayName("PUT /api/v1/recipes/{id}/ratings/me returns 200 for valid score")
    void rateRecipe_withValidScore_returns200() throws Exception {
        mockMvc.perform(put("/api/v1/recipes/{recipeId}/ratings/me", recipeId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(5));
    }

    @Test
    @DisplayName("PUT /api/v1/recipes/{id}/ratings/me updates existing rating")
    void rateRecipe_updateExistingRating_returns200() throws Exception {
        mockMvc.perform(put("/api/v1/recipes/{recipeId}/ratings/me", recipeId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":3}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/recipes/{recipeId}/ratings/me", recipeId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(5));
    }

    @Test
    @DisplayName("PUT /api/v1/recipes/{id}/ratings/me returns 400 for score out of range")
    void rateRecipe_withInvalidScore_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/recipes/{recipeId}/ratings/me", recipeId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":6}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/recipes/{id}/ratings/me returns 400 for zero score")
    void rateRecipe_withZeroScore_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/recipes/{recipeId}/ratings/me", recipeId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/recipes/{id}/ratings/me returns 401 without auth token")
    void rateRecipe_withoutAuth_returns401() throws Exception {
        mockMvc.perform(put("/api/v1/recipes/{recipeId}/ratings/me", recipeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":5}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/v1/recipes/{id}/ratings/me returns 404 when recipe not found")
    void rateRecipe_whenRecipeNotFound_returns404() throws Exception {
        mockMvc.perform(put("/api/v1/recipes/999/ratings/me")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":5}
                                """))
                .andExpect(status().isNotFound());
    }
}

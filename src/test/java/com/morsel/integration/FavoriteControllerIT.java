package com.morsel.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

@DisplayName("FavoriteController IT")
class FavoriteControllerIT extends AbstractIntegrationTest {

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
        SignUpResult userResult = signUp("favoriter", "fav@example.com", "Password1!");
        userToken = userResult.token();

        String createResponse = mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Favoritable Recipe","instructions":"Make it","ingredientIds":[%d]}
                                """.formatted(ingredient.getId())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        recipeId = ((Number) com.jayway.jsonpath.JsonPath.read(createResponse, "$.id")).longValue();
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{id}/favorite returns 201")
    void favoriteRecipe_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/{recipeId}/favorite", recipeId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("DELETE /api/v1/recipes/{id}/favorite returns 204")
    void unfavoriteRecipe_returns204() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/{recipeId}/favorite", recipeId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/recipes/{recipeId}/favorite", recipeId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/v1/users/me/favorites returns 200 with favorite recipes")
    void getFavorites_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/{recipeId}/favorite", recipeId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/users/me/favorites").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{id}/favorite returns 401 without auth token")
    void favoriteRecipe_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/{recipeId}/favorite", recipeId)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{id}/favorite returns 404 when recipe not found")
    void favoriteRecipe_whenRecipeNotFound_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/999/favorite").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }
}

package com.morsel.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.morsel.model.Ingredient;
import com.morsel.repository.CommentRepository;
import com.morsel.repository.IngredientRepository;
import com.morsel.repository.RecipeRepository;
import com.morsel.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

@DisplayName("CommentController IT")
class CommentControllerIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private CommentRepository commentRepository;

    private String userToken;
    private Long recipeId;

    @BeforeEach
    void setUp() throws Exception {
        commentRepository.deleteAll();
        recipeRepository.deleteAll();
        userRepository.deleteAll();
        ingredientRepository.deleteAll();

        Ingredient ingredient =
                ingredientRepository.save(Ingredient.builder().name("Tomato").build());
        SignUpResult userResult = signUp("commenter", "comment@example.com", "Password1!");
        userToken = userResult.token();

        String createResponse = mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Commentable Recipe","instructions":"Make it","ingredientIds":[%d]}
                                """.formatted(ingredient.getId())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        recipeId = ((Number) com.jayway.jsonpath.JsonPath.read(createResponse, "$.id")).longValue();
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{id}/comments returns 201 for authenticated user")
    void addComment_asAuthenticatedUser_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/{recipeId}/comments", recipeId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"Great recipe!"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Great recipe!"));
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{id}/comments returns 401 without auth token")
    void addComment_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/{recipeId}/comments", recipeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"No auth comment"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/recipes/{id}/comments returns 200 with comments list")
    void getComments_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/{recipeId}/comments", recipeId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"First comment"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/recipes/{recipeId}/comments", recipeId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"Second comment"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/recipes/{recipeId}/comments", recipeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{id}/comments returns 400 for missing text")
    void addComment_withMissingText_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/{recipeId}/comments", recipeId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{id}/comments returns 404 when recipe not found")
    void addComment_whenRecipeNotFound_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/999/comments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"Comment on nothing"}
                                """))
                .andExpect(status().isNotFound());
    }
}

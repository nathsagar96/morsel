package com.morsel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.morsel.dto.request.CreateRecipeRequest;
import com.morsel.dto.request.UpdateRecipeRequest;
import com.morsel.dto.response.RecipeResponse;
import com.morsel.exception.ForbiddenException;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.mapper.RecipeMapper;
import com.morsel.model.Ingredient;
import com.morsel.model.Recipe;
import com.morsel.model.Role;
import com.morsel.model.User;
import com.morsel.repository.IngredientRepository;
import com.morsel.repository.RecipeRepository;
import com.morsel.storage.FileStorageService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecipeService")
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private RecipeMapper recipeMapper;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private RecipeService recipeService;

    private User author;
    private User admin;
    private User otherUser;
    private Recipe recipe;
    private CreateRecipeRequest createRequest;
    private UpdateRecipeRequest updateRequest;
    private Ingredient ingredient;

    @BeforeEach
    void setUp() {
        author = User.builder().id(1L).username("author").role(Role.USER).build();
        admin = User.builder().id(2L).username("admin").role(Role.ADMIN).build();
        otherUser = User.builder().id(3L).username("other").role(Role.USER).build();
        ingredient = Ingredient.builder().id(10L).name("Salt").build();
        recipe = Recipe.builder()
                .id(100L)
                .title("Original Title")
                .description("Original desc")
                .instructions("Steps")
                .author(author)
                .ingredients(List.of(ingredient))
                .build();
        createRequest = new CreateRecipeRequest("New Title", "New desc", "New steps", null, List.of(10L));
        updateRequest = new UpdateRecipeRequest("Updated", "Updated desc", "Updated steps", null, List.of(10L));
    }

    @Test
    @DisplayName("creates recipe and returns response")
    void create_withValidRequest_returnsRecipeResponse() {
        when(ingredientRepository.findAllById(List.of(10L))).thenReturn(List.of(ingredient));
        when(recipeMapper.toEntity(createRequest, author, List.of(ingredient))).thenReturn(recipe);
        when(recipeRepository.save(recipe)).thenReturn(recipe);
        when(recipeMapper.toResponse(recipe)).thenReturn(RecipeResponse.of(recipe));

        RecipeResponse response = recipeService.create(createRequest, author);

        assertThat(response.title()).isEqualTo("Original Title");
        verify(recipeRepository).save(recipe);
    }

    @Test
    @DisplayName("creates recipe without ingredients")
    void create_withoutIngredients_returnsRecipeResponse() {
        CreateRecipeRequest noIngredientRequest = new CreateRecipeRequest("No Ing", "Desc", "Steps", null, null);
        Recipe noIngredientRecipe = Recipe.builder()
                .id(101L)
                .title("No Ing")
                .description("Desc")
                .instructions("Steps")
                .author(author)
                .ingredients(List.of())
                .build();

        when(recipeMapper.toEntity(noIngredientRequest, author, List.of())).thenReturn(noIngredientRecipe);
        when(recipeRepository.save(noIngredientRecipe)).thenReturn(noIngredientRecipe);
        when(recipeMapper.toResponse(noIngredientRecipe)).thenReturn(RecipeResponse.of(noIngredientRecipe));

        RecipeResponse response = recipeService.create(noIngredientRequest, author);

        assertThat(response.title()).isEqualTo("No Ing");
        verify(ingredientRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("returns paginated recipes")
    void findAll_returnsPagedResponses() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Recipe> recipePage = new PageImpl<>(List.of(recipe));
        when(recipeRepository.findAll(pageable)).thenReturn(recipePage);
        when(recipeMapper.toResponse(recipe)).thenReturn(RecipeResponse.of(recipe));

        Page<RecipeResponse> result = recipeService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().title()).isEqualTo("Original Title");
    }

    @Test
    @DisplayName("returns recipe by id")
    void findById_withExistingId_returnsRecipeResponse() {
        when(recipeRepository.findWithDetailsById(100L)).thenReturn(Optional.of(recipe));
        when(recipeMapper.toResponse(recipe)).thenReturn(RecipeResponse.of(recipe));

        RecipeResponse response = recipeService.findById(100L);

        assertThat(response.title()).isEqualTo("Original Title");
    }

    @Test
    @DisplayName("throws ResourceNotFoundException for non-existent id")
    void findById_withNonExistentId_throwsException() {
        when(recipeRepository.findWithDetailsById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recipeService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when ingredient IDs don't exist")
    void create_withNonExistentIngredientIds_throwsException() {
        when(ingredientRepository.findAllById(List.of(999L))).thenReturn(List.of());

        assertThatThrownBy(() -> recipeService.create(
                        new CreateRecipeRequest("Title", "Desc", "Steps", null, List.of(999L)), author))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(recipeRepository, never()).save(any());
    }

    @Test
    @DisplayName("updates recipe when owner")
    void update_byOwner_updatesAndReturnsResponse() {
        when(recipeRepository.findWithDetailsById(100L)).thenReturn(Optional.of(recipe));
        when(ingredientRepository.findAllById(List.of(10L))).thenReturn(List.of(ingredient));
        when(recipeMapper.toResponse(recipe)).thenReturn(RecipeResponse.of(recipe));

        RecipeResponse response = recipeService.update(100L, updateRequest, author);

        assertThat(response).isNotNull();
        verify(recipeMapper).updateEntity(recipe, updateRequest, List.of(ingredient));
    }

    @Test
    @DisplayName("throws ForbiddenException when non-owner tries to update")
    void update_byNonOwner_throwsForbidden() {
        when(recipeRepository.findWithDetailsById(100L)).thenReturn(Optional.of(recipe));

        assertThatThrownBy(() -> recipeService.update(100L, updateRequest, otherUser))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("do not own");

        verify(recipeRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when updating non-existent recipe")
    void update_withNonExistentId_throwsNotFound() {
        when(recipeRepository.findWithDetailsById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recipeService.update(999L, updateRequest, author))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("deletes recipe when admin")
    void delete_byAdmin_deletesRecipe() {
        when(recipeRepository.findWithDetailsById(100L)).thenReturn(Optional.of(recipe));

        recipeService.delete(100L, admin);

        verify(recipeRepository).delete(recipe);
    }

    @Test
    @DisplayName("throws ForbiddenException when non-admin tries to delete")
    void delete_byNonAdmin_throwsForbidden() {
        when(recipeRepository.findWithDetailsById(100L)).thenReturn(Optional.of(recipe));

        assertThatThrownBy(() -> recipeService.delete(100L, author))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only admins");

        verify(recipeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when deleting non-existent recipe")
    void delete_withNonExistentId_throwsNotFound() {
        when(recipeRepository.findWithDetailsById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recipeService.delete(999L, admin))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("uploads image and sets imageUrl on recipe")
    void uploadImage_byOwner_setsImageUrl() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        String expectedUrl = "/api/v1/images/uuid.jpg";
        RecipeResponse expectedResponse = new RecipeResponse(
                100L,
                "Original Title",
                "Original desc",
                "Steps",
                expectedUrl,
                1L,
                "author",
                List.of(10L),
                0.0,
                0,
                null,
                null);
        when(recipeRepository.findWithDetailsById(100L)).thenReturn(Optional.of(recipe));
        when(fileStorageService.store(file)).thenReturn(expectedUrl);
        when(recipeMapper.toResponse(recipe)).thenReturn(expectedResponse);

        RecipeResponse response = recipeService.uploadImage(100L, file, author);

        assertThat(response.imageUrl()).isEqualTo(expectedUrl);
        verify(fileStorageService).store(file);
    }

    @Test
    @DisplayName("throws ForbiddenException when non-owner uploads image")
    void uploadImage_byNonOwner_throwsForbidden() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(recipeRepository.findWithDetailsById(100L)).thenReturn(Optional.of(recipe));

        assertThatThrownBy(() -> recipeService.uploadImage(100L, file, otherUser))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("do not own");

        verify(fileStorageService, never()).store(any());
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when uploading image for non-existent recipe")
    void uploadImage_withNonExistentId_throwsNotFound() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(recipeRepository.findWithDetailsById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recipeService.uploadImage(999L, file, author))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(fileStorageService, never()).store(any());
    }
}

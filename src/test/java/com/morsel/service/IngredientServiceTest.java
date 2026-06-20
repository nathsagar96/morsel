package com.morsel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.morsel.dto.request.IngredientRequest;
import com.morsel.dto.response.IngredientResponse;
import com.morsel.exception.DuplicateResourceException;
import com.morsel.exception.ForbiddenException;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.mapper.IngredientMapper;
import com.morsel.model.Ingredient;
import com.morsel.model.Role;
import com.morsel.model.User;
import com.morsel.repository.IngredientRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("IngredientService")
class IngredientServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private IngredientMapper ingredientMapper;

    @InjectMocks
    private IngredientService ingredientService;

    private User regularUser;
    private User admin;
    private Ingredient ingredient;
    private IngredientResponse ingredientResponse;

    @BeforeEach
    void setUp() {
        regularUser = User.builder().id(1L).username("user").role(Role.USER).build();
        admin = User.builder().id(2L).username("admin").role(Role.ADMIN).build();
        ingredient = Ingredient.builder()
                .id(10L)
                .name("Tomato")
                .createdAt(Instant.parse("2025-06-15T10:30:00Z"))
                .updatedAt(Instant.parse("2025-06-20T14:45:00Z"))
                .build();
        ingredientResponse = new IngredientResponse(
                10L,
                "Tomato",
                Instant.parse("2025-06-15T10:30:00Z"),
                Instant.parse("2025-06-20T14:45:00Z"));
    }

    @Test
    @DisplayName("returns all ingredients")
    void findAll_returnsPagedResults() {
        PageRequest pageable = PageRequest.of(0, 20);
        Page<Ingredient> page = new PageImpl<>(List.of(ingredient));
        when(ingredientRepository.findAll(pageable)).thenReturn(page);
        when(ingredientMapper.toResponse(ingredient)).thenReturn(ingredientResponse);

        Page<IngredientResponse> result = ingredientService.findAll(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().name()).isEqualTo("Tomato");
    }

    @Test
    @DisplayName("searches ingredients by keyword")
    void findAll_withKeyword_returnsFilteredResults() {
        PageRequest pageable = PageRequest.of(0, 20);
        Page<Ingredient> page = new PageImpl<>(List.of(ingredient));
        when(ingredientRepository.findByNameContainingIgnoreCase("Tom", pageable)).thenReturn(page);
        when(ingredientMapper.toResponse(ingredient)).thenReturn(ingredientResponse);

        Page<IngredientResponse> result = ingredientService.findAll("Tom", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(ingredientRepository).findByNameContainingIgnoreCase("Tom", pageable);
    }

    @Test
    @DisplayName("returns ingredient by id")
    void findById_withExistingId_returnsResponse() {
        when(ingredientRepository.findById(10L)).thenReturn(Optional.of(ingredient));
        when(ingredientMapper.toResponse(ingredient)).thenReturn(ingredientResponse);

        IngredientResponse result = ingredientService.findById(10L);

        assertThat(result.name()).isEqualTo("Tomato");
    }

    @Test
    @DisplayName("throws ResourceNotFoundException for non-existent id")
    void findById_withNonExistentId_throwsException() {
        when(ingredientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingredientService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("creates ingredient and returns response")
    void create_withValidRequest_returnsResponse() {
        IngredientRequest request = new IngredientRequest("Basil");
        Ingredient newIngredient = Ingredient.builder().id(11L).name("Basil").build();
        IngredientResponse newResponse = new IngredientResponse(11L, "Basil", null, null);

        when(ingredientMapper.toEntity(request)).thenReturn(newIngredient);
        when(ingredientRepository.save(newIngredient)).thenReturn(newIngredient);
        when(ingredientMapper.toResponse(newIngredient)).thenReturn(newResponse);

        IngredientResponse result = ingredientService.create(request);

        assertThat(result.id()).isEqualTo(11L);
        assertThat(result.name()).isEqualTo("Basil");
        verify(ingredientRepository).save(newIngredient);
    }

    @Test
    @DisplayName("throws DuplicateResourceException on duplicate name")
    void create_withDuplicateName_throwsException() {
        IngredientRequest request = new IngredientRequest("Tomato");

        when(ingredientMapper.toEntity(request)).thenReturn(ingredient);
        when(ingredientRepository.save(ingredient)).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> ingredientService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(ingredientRepository).save(ingredient);
    }

    @Test
    @DisplayName("updates ingredient name")
    void update_withValidRequest_returnsResponse() {
        IngredientRequest request = new IngredientRequest("Cherry Tomato");
        when(ingredientRepository.findById(10L)).thenReturn(Optional.of(ingredient));
        when(ingredientRepository.save(any(Ingredient.class))).thenReturn(ingredient);
        when(ingredientMapper.toResponse(ingredient)).thenReturn(ingredientResponse);

        IngredientResponse result = ingredientService.update(10L, request);

        assertThat(result).isNotNull();
        verify(ingredientMapper).toResponse(ingredient);
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when updating non-existent ingredient")
    void update_withNonExistentId_throwsException() {
        IngredientRequest request = new IngredientRequest("New Name");
        when(ingredientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingredientService.update(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("throws DuplicateResourceException on update with duplicate name")
    void update_withDuplicateName_throwsException() {
        IngredientRequest request = new IngredientRequest("ExistingName");
        when(ingredientRepository.findById(10L)).thenReturn(Optional.of(ingredient));
        when(ingredientRepository.save(any(Ingredient.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> ingredientService.update(10L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("deletes ingredient when admin")
    void delete_byAdmin_deletes() {
        when(ingredientRepository.findById(10L)).thenReturn(Optional.of(ingredient));

        ingredientService.delete(10L, admin);

        verify(ingredientRepository).delete(ingredient);
    }

    @Test
    @DisplayName("throws ForbiddenException when non-admin tries to delete")
    void delete_byNonAdmin_throwsForbidden() {
        assertThatThrownBy(() -> ingredientService.delete(10L, regularUser))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only admins");

        verify(ingredientRepository, never()).delete(any());
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when deleting non-existent ingredient")
    void delete_withNonExistentId_throwsException() {
        when(ingredientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingredientService.delete(999L, admin))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(ingredientRepository, never()).delete(any());
    }
}

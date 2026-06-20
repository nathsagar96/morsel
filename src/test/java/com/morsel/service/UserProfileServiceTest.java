package com.morsel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.morsel.dto.response.UserProfileResponse;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.mapper.UserMapper;
import com.morsel.model.Role;
import com.morsel.model.User;
import com.morsel.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileService")
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserProfileService userProfileService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encoded")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("returns profile for existing username")
    void getProfile_withExistingUsername_returnsProfile() {
        when(userRepository.findWithRecipesByUsername("testuser")).thenReturn(Optional.of(user));
        when(userMapper.toProfileResponse(user)).thenReturn(new UserProfileResponse(1L, "testuser", List.of()));

        UserProfileResponse response = userProfileService.getProfile("testuser");

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("throws ResourceNotFoundException for unknown username")
    void getProfile_withUnknownUsername_throwsNotFound() {
        when(userRepository.findWithRecipesByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getProfile("unknown"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("unknown");
    }
}

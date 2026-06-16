package com.morsel.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.morsel.TestcontainersConfiguration;
import com.morsel.model.Role;
import com.morsel.model.User;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@DisplayName("UserRepository")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("encoded-password")
                .role(Role.USER)
                .build());
    }

    @Test
    @DisplayName("finds user by username when exists")
    void findByUsername_whenExists_returnsUser() {
        Optional<User> result = userRepository.findByUsername("testuser");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isNotNull();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        assertThat(result.get().getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("returns empty when username not found")
    void findByUsername_whenNotExists_returnsEmpty() {
        Optional<User> result = userRepository.findByUsername("nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("finds user by email when exists")
    void findByEmail_whenExists_returnsUser() {
        Optional<User> result = userRepository.findByEmail("test@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("returns empty when email not found")
    void findByEmail_whenNotExists_returnsEmpty() {
        Optional<User> result = userRepository.findByEmail("unknown@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("checks username existence returns true when exists")
    void existsByUsername_whenExists_returnsTrue() {
        assertThat(userRepository.existsByUsername("testuser")).isTrue();
    }

    @Test
    @DisplayName("checks username existence returns false when not exists")
    void existsByUsername_whenNotExists_returnsFalse() {
        assertThat(userRepository.existsByUsername("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("checks email existence returns true when exists")
    void existsByEmail_whenExists_returnsTrue() {
        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
    }

    @Test
    @DisplayName("checks email existence returns false when not exists")
    void existsByEmail_whenNotExists_returnsFalse() {
        assertThat(userRepository.existsByEmail("unknown@example.com")).isFalse();
    }
}

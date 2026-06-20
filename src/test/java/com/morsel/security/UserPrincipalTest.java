package com.morsel.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.morsel.model.Role;
import com.morsel.model.User;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

@DisplayName("UserPrincipal")
class UserPrincipalTest {

    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .role(Role.USER)
                .build();
        userPrincipal = new UserPrincipal(user);
    }

    @Test
    @DisplayName("returns ROLE_USER authority for USER role")
    void getAuthorities_returnsRoleUser() {
        Collection<? extends GrantedAuthority> authorities = userPrincipal.getAuthorities();

        assertThat(authorities).hasSize(1);
        assertThat(authorities).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("returns ROLE_ADMIN authority for ADMIN role")
    void getAuthorities_whenAdmin_returnsRoleAdmin() {
        User admin = User.builder()
                .id(2L)
                .username("admin")
                .email("admin@example.com")
                .password("admin-password")
                .role(Role.ADMIN)
                .build();
        UserPrincipal adminPrincipal = new UserPrincipal(admin);

        Collection<? extends GrantedAuthority> authorities = adminPrincipal.getAuthorities();

        assertThat(authorities).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("returns user password")
    void getPassword_returnsUserPassword() {
        assertThat(userPrincipal.getPassword()).isEqualTo("password");
    }

    @Test
    @DisplayName("returns user username")
    void getUsername_returnsUserUsername() {
        assertThat(userPrincipal.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("account is not expired")
    void isAccountNonExpired_returnsTrue() {
        assertThat(userPrincipal.isAccountNonExpired()).isTrue();
    }

    @Test
    @DisplayName("returns true when account is not locked")
    void isAccountNonLocked_whenNotLocked_returnsTrue() {
        assertThat(userPrincipal.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("returns false when account is locked")
    void isAccountNonLocked_whenLocked_returnsFalse() {
        User lockedUser = User.builder()
                .id(2L)
                .username("lockeduser")
                .email("locked@example.com")
                .password("password")
                .role(Role.USER)
                .accountNonLocked(false)
                .build();
        UserPrincipal lockedPrincipal = new UserPrincipal(lockedUser);

        assertThat(lockedPrincipal.isAccountNonLocked()).isFalse();
    }

    @Test
    @DisplayName("credentials are not expired")
    void isCredentialsNonExpired_returnsTrue() {
        assertThat(userPrincipal.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("returns true when account is enabled")
    void isEnabled_whenEnabled_returnsTrue() {
        assertThat(userPrincipal.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("returns false when account is disabled")
    void isEnabled_whenDisabled_returnsFalse() {
        User disabledUser = User.builder()
                .id(2L)
                .username("disableduser")
                .email("disabled@example.com")
                .password("password")
                .role(Role.USER)
                .enabled(false)
                .build();
        UserPrincipal disabledPrincipal = new UserPrincipal(disabledUser);

        assertThat(disabledPrincipal.isEnabled()).isFalse();
    }
}

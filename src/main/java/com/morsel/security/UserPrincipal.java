package com.morsel.security;

import com.morsel.constants.AuthConstants;
import com.morsel.model.Role;
import com.morsel.model.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record UserPrincipal(User user) implements UserDetails {

    public static UserPrincipal fromClaims(JwtTokenProvider.AccessTokenClaims claims) {
        User user = User.builder()
                .id(claims.userId())
                .username(claims.username())
                .role(Role.valueOf(claims.role()))
                .enabled(claims.enabled())
                .accountNonLocked(claims.accountNonLocked())
                .password("")
                .version(0L)
                .build();
        return new UserPrincipal(user);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(
                AuthConstants.ROLE_PREFIX + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}

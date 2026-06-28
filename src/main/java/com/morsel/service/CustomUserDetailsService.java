package com.morsel.service;

import com.morsel.logging.PiiSanitizer;
import com.morsel.repository.UserRepository;
import com.morsel.security.UserPrincipal;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Observed
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(@NonNull String usernameOrEmail) throws UsernameNotFoundException {
        log.debug("Loading user by username or email: {}", PiiSanitizer.sanitizeIdentifier(usernameOrEmail));
        return userRepository
                .findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .map(UserPrincipal::new)
                .orElseThrow(() -> {
                    log.debug(
                            "User not found with username or email: {}",
                            PiiSanitizer.sanitizeIdentifier(usernameOrEmail));
                    return new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail);
                });
    }
}

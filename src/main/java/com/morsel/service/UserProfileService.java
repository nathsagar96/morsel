package com.morsel.service;

import com.morsel.dto.response.UserProfileResponse;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.logging.AuditLogger;
import com.morsel.logging.AuditLogger.Event;
import com.morsel.logging.AuditLogger.Outcome;
import com.morsel.logging.PiiSanitizer;
import com.morsel.mapper.UserMapper;
import com.morsel.model.User;
import com.morsel.repository.RecipeRepository;
import com.morsel.repository.UserRepository;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Observed
public class UserProfileService {

    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> {
            log.warn("User profile not found: {}", PiiSanitizer.sanitizeIdentifier(username));
            return new ResourceNotFoundException("User not found: " + username);
        });
        int recipeCount = Math.toIntExact(recipeRepository.countByAuthorId(user.getId()));
        log.debug("Profile fetched for user: {}", PiiSanitizer.sanitizeIdentifier(username));
        return userMapper.toProfileResponse(user, recipeCount);
    }

    @Transactional
    public void updateUserStatus(Long id, boolean enabled) {
        User user =
                userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setEnabled(enabled);
        userRepository.save(user);
        log.info("User {} {} by admin", id, enabled ? "enabled" : "disabled");
        AuditLogger.log(Event.ADMIN_USER_STATUS_CHANGE, id, Outcome.SUCCESS, "enabled=" + enabled);
    }
}

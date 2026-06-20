package com.morsel.service;

import com.morsel.dto.response.UserProfileResponse;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.mapper.UserMapper;
import com.morsel.model.User;
import com.morsel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username) {
        User user = userRepository.findWithRecipesByUsername(username).orElseThrow(() -> {
            log.warn("User profile not found: {}", username);
            return new ResourceNotFoundException("User not found: " + username);
        });
        log.debug("Profile fetched for user: {}", username);
        return userMapper.toProfileResponse(user);
    }
}

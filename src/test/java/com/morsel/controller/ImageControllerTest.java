package com.morsel.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.morsel.exception.ResourceNotFoundException;
import com.morsel.security.JwtTokenProvider;
import com.morsel.service.CustomUserDetailsService;
import com.morsel.storage.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = ImageController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ImageController")
class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileStorageService fileStorageService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("GET /api/v1/images/{filename} returns 200 with image content")
    void serveImage_withExistingFile_returns200() throws Exception {
        when(fileStorageService.load("test.jpg")).thenReturn(new ByteArrayResource("image-content".getBytes()));

        mockMvc.perform(get("/api/v1/images/test.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes("image-content".getBytes()));
    }

    @Test
    @DisplayName("GET /api/v1/images/{filename} returns 404 when not found")
    void serveImage_withNonExistentFile_returns404() throws Exception {
        when(fileStorageService.load(anyString()))
                .thenThrow(new ResourceNotFoundException("Image not found: nonexistent.jpg"));

        mockMvc.perform(get("/api/v1/images/nonexistent.jpg")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/images/{filename} returns correct content type for png")
    void serveImage_png_returnsPngContentType() throws Exception {
        when(fileStorageService.load("photo.png")).thenReturn(new ByteArrayResource("png-content".getBytes()));

        mockMvc.perform(get("/api/v1/images/photo.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }
}

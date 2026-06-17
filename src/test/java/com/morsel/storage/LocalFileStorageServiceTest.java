package com.morsel.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.morsel.config.StorageProperties;
import com.morsel.exception.InvalidFileException;
import com.morsel.exception.ResourceNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("LocalFileStorageService")
class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService storageService;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties(tempDir.toString());
        storageService = new LocalFileStorageService(properties);
        storageService.init();
    }

    @Test
    @DisplayName("stores file and returns URL path")
    void store_withValidFile_returnsUrlPath() {
        MultipartFile file = createMockFile("photo.jpg", "image/jpeg", "fake-image-content".getBytes());

        String url = storageService.store(file);

        assertThat(url).startsWith("/api/v1/images/").endsWith(".jpg");
    }

    @Test
    @DisplayName("throws InvalidFileException for empty file")
    void store_withEmptyFile_throwsException() {
        MultipartFile file = createMockFile("empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("throws InvalidFileException for null file")
    void store_withNullFile_throwsException() {
        assertThatThrownBy(() -> storageService.store(null))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("throws InvalidFileException for disallowed file type")
    void store_withDisallowedExtension_throwsException() {
        MultipartFile file = createMockFile("document.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Invalid file type");
    }

    @Test
    @DisplayName("loads stored file successfully")
    void load_withExistingFile_returnsResource() throws IOException {
        MultipartFile file = createMockFile("photo.png", "image/png", "png-content".getBytes());
        String url = storageService.store(file);
        String filename = url.substring(url.lastIndexOf('/') + 1);

        Resource resource = storageService.load(filename);

        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
        assertThat(resource.getContentAsByteArray()).isEqualTo("png-content".getBytes());
    }

    @Test
    @DisplayName("throws ResourceNotFoundException for non-existent file")
    void load_withNonExistentFile_throwsException() {
        assertThatThrownBy(() -> storageService.load("nonexistent.jpg"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Image not found");
    }

    @Test
    @DisplayName("throws InvalidFileException for path traversal attempt")
    void load_withPathTraversal_throwsException() {
        assertThatThrownBy(() -> storageService.load("../../etc/passwd"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("outside upload directory");
    }

    private MultipartFile createMockFile(String filename, String contentType, byte[] content) {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        given(file.getOriginalFilename()).willReturn(filename);
        given(file.getContentType()).willReturn(contentType);
        given(file.isEmpty()).willReturn(content.length == 0);
        try {
            given(file.getInputStream()).willReturn(new ByteArrayInputStream(content));
            given(file.getSize()).willReturn((long) content.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }
}

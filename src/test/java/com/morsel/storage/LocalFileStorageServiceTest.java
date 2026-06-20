package com.morsel.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.morsel.config.StorageProperties;
import com.morsel.exception.InvalidFileException;
import com.morsel.exception.ResourceNotFoundException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("LocalFileStorageService")
class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService storageService;

    private static final byte[] MINIMAL_WEBP = HexFormat.of()
            .parseHex("52494646" + "3c000000" + "57454250" + "56503820" + "30000000" + "d001009d" + "012a0100"
                    + "01000140" + "2625a002" + "74ba01f8" + "0003b000" + "fef2eb7f" + "fcd815cd"
                    + "73eff7ff" + "d2e0fd2e" + "0fd2e0ff" + "d2900000");

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties(tempDir.toString());
        storageService = new LocalFileStorageService(properties);
        storageService.init();
    }

    @Test
    @DisplayName("stores JPEG file and returns URL path with transcoded content")
    void store_withJpeg_returnsUrlPath() throws IOException {
        MultipartFile file = createMockFile("photo.jpg", "image/jpeg", createValidImageBytes("jpeg"));

        String url = storageService.store(file);

        assertThat(url).startsWith("/api/v1/images/").endsWith(".jpg");
        assertThat(Files.list(tempDir).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("stores PNG file and returns URL path")
    void store_withPng_returnsUrlPath() {
        MultipartFile file = createMockFile("photo.png", "image/png", createValidImageBytes("png"));

        String url = storageService.store(file);

        assertThat(url).startsWith("/api/v1/images/").endsWith(".png");
    }

    @Test
    @DisplayName("stores GIF file and returns URL path")
    void store_withGif_returnsUrlPath() {
        MultipartFile file = createMockFile("photo.gif", "image/gif", createValidImageBytes("gif"));

        String url = storageService.store(file);

        assertThat(url).startsWith("/api/v1/images/").endsWith(".gif");
    }

    @Test
    @DisplayName("stores WebP file and returns URL path")
    void store_withWebp_returnsUrlPath() {
        MultipartFile file = createMockFile("photo.webp", "image/webp", MINIMAL_WEBP);

        String url = storageService.store(file);

        assertThat(url).startsWith("/api/v1/images/").endsWith(".webp");
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

    @ParameterizedTest
    @ValueSource(strings = {"image.svg", "photo.SVG", "icon.Svg"})
    @DisplayName("throws InvalidFileException for SVG extension")
    void store_withSvgExtension_throwsException(String filename) {
        MultipartFile file = createMockFile(filename, "image/svg+xml", "<svg></svg>".getBytes());

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Invalid file type");
    }

    @Test
    @DisplayName("throws InvalidFileException for file with mismatched extension and content")
    void store_withMismatchedContent_throwsException() {
        byte[] textBytes = "this is not an image".getBytes();
        MultipartFile file = createMockFile("photo.jpg", "image/jpeg", textBytes);

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    @DisplayName("throws InvalidFileException for file that is too small")
    void store_withTooSmallFile_throwsException() {
        MultipartFile file = createMockFile("tiny.jpg", "image/jpeg", new byte[] {(byte) 0xFF});

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("too small");
    }

    @Test
    @DisplayName("JPEG transcoding produces different output than input")
    void store_transcodedJpeg_differsFromOriginal() throws IOException {
        byte[] inputBytes = createValidImageBytes("jpeg");
        MultipartFile file = createMockFile("photo.jpg", "image/jpeg", inputBytes);

        String url = storageService.store(file);
        String filename = url.substring(url.lastIndexOf('/') + 1);
        Path storedPath = tempDir.resolve(filename);
        byte[] storedBytes = Files.readAllBytes(storedPath);

        assertThat(storedBytes).isNotEqualTo(inputBytes);
    }

    @Test
    @DisplayName("PNG transcoding produces valid PNG output")
    void store_transcodedPng_producesValidPng() throws IOException {
        byte[] inputBytes = createValidImageBytes("png");
        MultipartFile file = createMockFile("photo.png", "image/png", inputBytes);

        String url = storageService.store(file);
        String filename = url.substring(url.lastIndexOf('/') + 1);
        Path storedPath = tempDir.resolve(filename);
        byte[] storedBytes = Files.readAllBytes(storedPath);

        assertThat(storedBytes.length).isGreaterThan(8);
        assertThat(storedBytes[0]).isEqualTo((byte) 0x89);
        assertThat(storedBytes[1]).isEqualTo((byte) 0x50);
        assertThat(storedBytes[2]).isEqualTo((byte) 0x4E);
        assertThat(storedBytes[3]).isEqualTo((byte) 0x47);
    }

    @Test
    @DisplayName("loads stored file successfully")
    void load_withExistingFile_returnsResource() throws IOException {
        MultipartFile file = createMockFile("photo.png", "image/png", createValidImageBytes("png"));
        String url = storageService.store(file);
        String filename = url.substring(url.lastIndexOf('/') + 1);

        Resource resource = storageService.load(filename);

        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
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

    private byte[] createValidImageBytes(String format) {
        try {
            BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            img.setRGB(0, 0, 0xFF0000);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, format, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test image bytes", e);
        }
    }

    private MultipartFile createMockFile(String filename, String contentType, byte[] content) {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        given(file.getOriginalFilename()).willReturn(filename);
        given(file.getContentType()).willReturn(contentType);
        given(file.isEmpty()).willReturn(content.length == 0);
        try {
            given(file.getInputStream()).willReturn(new ByteArrayInputStream(content));
            given(file.getBytes()).willReturn(content);
            given(file.getSize()).willReturn((long) content.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }
}

package com.morsel.storage;

import com.morsel.config.StorageProperties;
import com.morsel.exception.InvalidFileException;
import com.morsel.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "webp", "svg");
    private static final String IMAGE_URL_PREFIX = "/api/v1/images/";

    private final StorageProperties storageProperties;
    private Path uploadDir;

    @PostConstruct
    void init() {
        uploadDir = Paths.get(storageProperties.uploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDir);
            log.info("File upload directory created/verified: {}", uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory: " + uploadDir, e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Uploaded file is empty");
        }
        String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "");
        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new InvalidFileException(
                    "Invalid file type: ." + extension + ". Allowed: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
        String storedFilename = UUID.randomUUID() + "." + extension;
        Path targetPath = uploadDir.resolve(storedFilename);
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored: {}", targetPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file: " + storedFilename, e);
        }
        return IMAGE_URL_PREFIX + storedFilename;
    }

    @Override
    public Resource load(String filename) {
        try {
            Path filePath = uploadDir.resolve(filename).normalize();
            if (!filePath.startsWith(uploadDir)) {
                throw new InvalidFileException("Cannot access file outside upload directory");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new ResourceNotFoundException("Image not found: " + filename);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Failed to load file: " + filename, e);
        }
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1);
    }
}

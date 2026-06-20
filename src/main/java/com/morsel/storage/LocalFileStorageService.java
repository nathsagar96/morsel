package com.morsel.storage;

import com.morsel.config.StorageProperties;
import com.morsel.exception.InvalidFileException;
import com.morsel.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
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

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "webp");
    private static final String IMAGE_URL_PREFIX = "/api/v1/images/";

    private static final byte[] JPEG_MAGIC = HexFormat.of().parseHex("ffd8ff");
    private static final byte[] PNG_MAGIC = HexFormat.of().parseHex("89504e470d0a1a0a");
    private static final byte[] GIF87A_MAGIC = HexFormat.of().parseHex("474946383761");
    private static final byte[] GIF89A_MAGIC = HexFormat.of().parseHex("474946383961");
    private static final byte[] RIFF_MAGIC = HexFormat.of().parseHex("52494646");
    private static final byte[] WEBP_MAGIC = HexFormat.of().parseHex("57454250");

    private static final float JPEG_QUALITY = 0.70f;

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
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        }
        validateMagicBytes(fileBytes, extension);
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(fileBytes));
        } catch (IOException e) {
            throw new InvalidFileException("Failed to decode image: " + e.getMessage());
        }
        if (image == null) {
            throw new InvalidFileException("File content is not a valid image");
        }
        String storedFilename = UUID.randomUUID() + "." + extension;
        Path targetPath = uploadDir.resolve(storedFilename);
        if ("webp".equals(extension.toLowerCase())) {
            storeAsIs(fileBytes, targetPath);
            log.info("Image stored (raw): {}", targetPath);
        } else {
            transcodeImage(image, extension, targetPath);
            log.info("Image stored and transcoded: {}", targetPath);
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

    private void validateMagicBytes(byte[] bytes, String extension) {
        if (bytes.length < 12) {
            throw new InvalidFileException("File too small to be a valid image");
        }
        boolean valid =
                switch (extension.toLowerCase()) {
                    case "jpeg", "jpg" -> startsWith(bytes, JPEG_MAGIC);
                    case "png" -> startsWith(bytes, PNG_MAGIC);
                    case "gif" -> startsWith(bytes, GIF87A_MAGIC) || startsWith(bytes, GIF89A_MAGIC);
                    case "webp" -> startsWith(bytes, RIFF_MAGIC) && startsWithOffset(bytes, WEBP_MAGIC, 8);
                    default -> false;
                };
        if (!valid) {
            throw new InvalidFileException(
                    "File content does not match ." + extension + " format — possible extension spoofing");
        }
    }

    private void storeAsIs(byte[] fileBytes, Path targetPath) {
        try {
            Files.write(targetPath, fileBytes);
        } catch (IOException e) {
            throw new InvalidFileException("Failed to store image: " + e.getMessage());
        }
    }

    private void transcodeImage(BufferedImage image, String extension, Path targetPath) {
        String formatName = extension.equals("jpg") ? "jpeg" : extension;
        try {
            if ("jpeg".equals(formatName)) {
                transcodeJpeg(image, targetPath);
            } else {
                if (!ImageIO.write(image, formatName, targetPath.toFile())) {
                    throw new InvalidFileException("No image writer available for format: " + formatName);
                }
            }
        } catch (IOException e) {
            throw new InvalidFileException("Failed to transcode image: " + e.getMessage());
        }
    }

    private void transcodeJpeg(BufferedImage image, Path targetPath) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.setOutput(new FileImageOutputStream(targetPath.toFile()));
            writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        return startsWithOffset(data, prefix, 0);
    }

    private static boolean startsWithOffset(byte[] data, byte[] prefix, int offset) {
        if (data.length < offset + prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1);
    }
}

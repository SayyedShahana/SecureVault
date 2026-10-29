
package com.securevault.securevault.service;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentValidationService {

    private final long maxFileSizeBytes;

    private static final Map<String, String> ALLOWED_EXTENSIONS = Map.of(
            "pdf", "application/pdf",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg");

    public DocumentValidationService(
            @Value("${securevault.storage.max-file-size-bytes}") long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public void validate(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty.");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("File size exceeds the 10 MB limit.");
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null
                || originalFilename.isBlank()
                || originalFilename.contains("/")
                || originalFilename.contains("\\")
                || originalFilename.contains("..")) {
            throw new IllegalArgumentException("Invalid filename.");
        }

        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == originalFilename.length() - 1) {
            throw new IllegalArgumentException("Unsupported file extension.");
        }

        String extension = originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);

        String expectedMimeType = ALLOWED_EXTENSIONS.get(extension);

        if (expectedMimeType == null) {
            throw new IllegalArgumentException("File type is not allowed.");
        }

        String suppliedMimeType = file.getContentType();

        if (suppliedMimeType == null
                || !expectedMimeType.equalsIgnoreCase(suppliedMimeType)) {
            throw new IllegalArgumentException("File content type does not match its extension.");
        }

        byte[] header = file.getBytes();

        if (!hasValidSignature(extension, header)) {
            throw new IllegalArgumentException("File content does not match the allowed type.");
        }
    }

    private boolean hasValidSignature(String extension, byte[] bytes) {
        switch (extension) {
            case "pdf":
                return startsWith(bytes, new byte[] { 0x25, 0x50, 0x44, 0x46, 0x2D });

            case "png":
                return startsWith(
                        bytes,
                        new byte[] {
                                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
                        });

            case "jpg":
            case "jpeg":
                return bytes.length >= 3
                        && bytes[0] == (byte) 0xFF
                        && bytes[1] == (byte) 0xD8
                        && bytes[2] == (byte) 0xFF;

            default:
                return false;
        }
    }

    private boolean startsWith(byte[] bytes, byte[] signature) {
        if (bytes.length < signature.length) {
            return false;
        }

        for (int i = 0; i < signature.length; i++) {
            if (bytes[i] != signature[i]) {
                return false;
            }
        }

        return true;
    }
}

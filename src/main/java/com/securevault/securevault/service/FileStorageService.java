
package com.securevault.securevault.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final Path storageLocation;

    public FileStorageService(
            @Value("${securevault.storage.location}") String storagePath) {
        this.storageLocation = Path.of(storagePath).toAbsolutePath().normalize();
    }

    public UUID storeFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty.");
        }

        Files.createDirectories(storageLocation);

        UUID storageKey = UUID.randomUUID();
        Path destination = storageLocation.resolve(storageKey.toString()).normalize();

        if (!destination.getParent().equals(storageLocation)) {
            throw new IOException("Invalid storage destination.");
        }

        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        return storageKey;
    }

    public Path getFilePath(UUID storageKey) {
        if (storageKey == null) {
            throw new IllegalArgumentException("Storage key must not be null.");
        }

        Path filePath = storageLocation.resolve(storageKey.toString()).normalize();

        if (!filePath.getParent().equals(storageLocation)) {
            throw new IllegalArgumentException("Invalid storage key.");
        }

        return filePath;
    }

    public void deleteFile(UUID storageKey) throws IOException {
        Files.deleteIfExists(getFilePath(storageKey));
    }
}

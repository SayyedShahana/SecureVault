package com.securevault.securevault.service;

import com.securevault.securevault.common.MessageKeys;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

  private final Path storageLocation;
  private final MessageSource messageSource;

  public FileStorageService(
      @Value("${securevault.storage.location}") String storagePath, MessageSource messageSource) {
    this.storageLocation = Path.of(storagePath).toAbsolutePath().normalize();
    this.messageSource = messageSource;
  }

  public UUID storeFile(MultipartFile file) throws IOException {

    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException(getMessage(MessageKeys.DOCUMENT_VALIDATION_EMPTY));
    }

    Files.createDirectories(storageLocation);

    UUID storageKey = UUID.randomUUID();
    Path destination = storageLocation.resolve(storageKey.toString()).normalize();

    if (!destination.getParent().equals(storageLocation)) {
      throw new IOException(getMessage(MessageKeys.STORAGE_DESTINATION_INVALID));
    }

    try (InputStream inputStream = file.getInputStream()) {
      Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    return storageKey;
  }

  public Path getFilePath(UUID storageKey) {

    if (storageKey == null) {
      throw new IllegalArgumentException(getMessage(MessageKeys.STORAGE_KEY_REQUIRED));
    }

    Path filePath = storageLocation.resolve(storageKey.toString()).normalize();

    if (!filePath.getParent().equals(storageLocation)) {
      throw new IllegalArgumentException(getMessage(MessageKeys.STORAGE_KEY_INVALID));
    }

    return filePath;
  }

  public void deleteFile(UUID storageKey) throws IOException {
    Files.deleteIfExists(getFilePath(storageKey));
  }

  private String getMessage(String messageKey) {
    return messageSource.getMessage(messageKey, null, Locale.getDefault());
  }
}

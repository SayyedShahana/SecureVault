package com.securevault.securevault.service;

import com.securevault.securevault.common.MessageKeys;
import com.securevault.securevault.entity.Document;
import com.securevault.securevault.entity.User;
import com.securevault.securevault.exception.DocumentNotFoundException;
import com.securevault.securevault.repository.DocumentRepository;
import com.securevault.securevault.repository.UserRepository;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

  private final DocumentRepository documentRepository;
  private final UserRepository userRepository;
  private final DocumentValidationService validationService;
  private final FileStorageService fileStorageService;
  private final MessageSource messageSource;

  public DocumentService(
      DocumentRepository documentRepository,
      UserRepository userRepository,
      DocumentValidationService validationService,
      FileStorageService fileStorageService,
      MessageSource messageSource) {
    this.documentRepository = documentRepository;
    this.userRepository = userRepository;
    this.validationService = validationService;
    this.fileStorageService = fileStorageService;
    this.messageSource = messageSource;
  }

  @Transactional
  public Document uploadDocument(String email, MultipartFile file) throws IOException {
    validationService.validate(file);

    User owner =
        userRepository
            .findByEmail(email)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        getMessage(MessageKeys.USER_AUTHENTICATED_NOT_FOUND)));

    UUID storageKey = fileStorageService.storeFile(file);

    try {
      Document document = new Document();
      document.setOwner(owner);
      document.setFileName(file.getOriginalFilename());
      document.setStorageKey(storageKey);
      document.setMimeType(file.getContentType());
      document.setFileSizeBytes(file.getSize());
      document.setFileHash(calculateSha256(file));
      document.setStatus("ACTIVE");

      return documentRepository.save(document);
    } catch (RuntimeException ex) {
      try {
        fileStorageService.deleteFile(storageKey);
      } catch (IOException cleanupException) {
        ex.addSuppressed(cleanupException);
      }
      throw ex;
    }
  }

  @Transactional(readOnly = true)
  public Document getDocumentForDownload(String email, UUID documentId) {
    User owner =
        userRepository
            .findByEmail(email)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        getMessage(MessageKeys.USER_AUTHENTICATED_NOT_FOUND)));

    return documentRepository
        .findByIdAndOwner_IdAndStatus(documentId, owner.getId(), "ACTIVE")
        .orElseThrow(
            () -> new DocumentNotFoundException(getMessage(MessageKeys.DOCUMENT_NOT_FOUND)));
  }

  @Transactional(readOnly = true)
  public Document getDocumentForPreview(String email, UUID documentId) {
    User owner =
        userRepository
            .findByEmail(email)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        getMessage(MessageKeys.USER_AUTHENTICATED_NOT_FOUND)));

    Document document =
        documentRepository
            .findByIdAndOwner_IdAndStatus(documentId, owner.getId(), "ACTIVE")
            .orElseThrow(
                () -> new DocumentNotFoundException(getMessage(MessageKeys.DOCUMENT_NOT_FOUND)));

    if (!validationService.isPreviewSupported(document.getMimeType())) {
      throw new IllegalArgumentException(getMessage(MessageKeys.DOCUMENT_PREVIEW_UNSUPPORTED));
    }

    return document;
  }

  @Transactional(readOnly = true)
  public List<Document> listDocuments(
      String email, String search, String mimeType, String sortBy, String direction) {

    User owner =
        userRepository
            .findByEmail(email)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        getMessage(MessageKeys.USER_AUTHENTICATED_NOT_FOUND)));

    String normalizedSearch = search == null ? "" : search.trim();
    String normalizedMimeType = mimeType == null ? "" : mimeType.trim();

    String normalizedSortBy = sortBy == null || sortBy.isBlank() ? "createdAt" : sortBy.trim();

    if (!normalizedSortBy.equals("fileName") && !normalizedSortBy.equals("createdAt")) {
      throw new IllegalArgumentException(getMessage(MessageKeys.DOCUMENT_SORT_INVALID_FIELD));
    }

    Sort.Direction sortDirection;

    try {
      sortDirection =
          direction == null || direction.isBlank()
              ? Sort.Direction.DESC
              : Sort.Direction.fromString(direction.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(getMessage(MessageKeys.DOCUMENT_SORT_INVALID_DIRECTION));
    }

    Sort sort = Sort.by(sortDirection, normalizedSortBy).and(Sort.by(Sort.Direction.ASC, "id"));

    boolean hasSearch = !normalizedSearch.isEmpty();
    boolean hasMimeType = !normalizedMimeType.isEmpty();

    if (hasSearch && hasMimeType) {
      return documentRepository.findByOwner_IdAndStatusAndFileNameContainingIgnoreCaseAndMimeType(
          owner.getId(), "ACTIVE", normalizedSearch, normalizedMimeType, sort);
    }

    if (hasSearch) {
      return documentRepository.findByOwner_IdAndStatusAndFileNameContainingIgnoreCase(
          owner.getId(), "ACTIVE", normalizedSearch, sort);
    }

    if (hasMimeType) {
      return documentRepository.findByOwner_IdAndStatusAndMimeType(
          owner.getId(), "ACTIVE", normalizedMimeType, sort);
    }

    return documentRepository.findByOwner_IdAndStatus(owner.getId(), "ACTIVE", sort);
  }

  @Transactional
  public Document renameDocument(String email, UUID documentId, String newFileName) {
    User owner =
        userRepository
            .findByEmail(email)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        getMessage(MessageKeys.USER_AUTHENTICATED_NOT_FOUND)));

    Document document =
        documentRepository
            .findByIdAndOwner_IdAndStatus(documentId, owner.getId(), "ACTIVE")
            .orElseThrow(
                () -> new DocumentNotFoundException(getMessage(MessageKeys.DOCUMENT_NOT_FOUND)));

    if (newFileName == null || newFileName.isBlank()) {
      throw new IllegalArgumentException(getMessage(MessageKeys.DOCUMENT_RENAME_FILENAME_EMPTY));
    }

    String normalizedFileName = newFileName.trim();

    if (normalizedFileName.length() > 255
        || normalizedFileName.contains("/")
        || normalizedFileName.contains("\\")
        || normalizedFileName.contains("..")) {
      throw new IllegalArgumentException(getMessage(MessageKeys.DOCUMENT_RENAME_INVALID_FILENAME));
    }

    int oldExtensionIndex = document.getFileName().lastIndexOf('.');
    int newExtensionIndex = normalizedFileName.lastIndexOf('.');

    if (oldExtensionIndex <= 0
        || newExtensionIndex <= 0
        || !document
            .getFileName()
            .substring(oldExtensionIndex)
            .equalsIgnoreCase(normalizedFileName.substring(newExtensionIndex))) {
      throw new IllegalArgumentException(getMessage(MessageKeys.DOCUMENT_RENAME_EXTENSION_CHANGE));
    }

    document.setFileName(normalizedFileName);

    return documentRepository.save(document);
  }

  @Transactional
  public void deleteDocument(String email, UUID documentId) throws IOException {
    User owner =
        userRepository
            .findByEmail(email)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        getMessage(MessageKeys.USER_AUTHENTICATED_NOT_FOUND)));

    Document document =
        documentRepository
            .findByIdAndOwner_IdAndStatus(documentId, owner.getId(), "ACTIVE")
            .orElseThrow(
                () -> new DocumentNotFoundException(getMessage(MessageKeys.DOCUMENT_NOT_FOUND)));

    fileStorageService.deleteFile(document.getStorageKey());

    document.setStatus("DELETED");

    documentRepository.save(document);
  }

  private String calculateSha256(MultipartFile file) throws IOException {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(file.getBytes());
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(getMessage(MessageKeys.CRYPTO_SHA256_UNAVAILABLE), ex);
    }
  }

  private String getMessage(String messageKey) {
    return messageSource.getMessage(messageKey, null, Locale.getDefault());
  }
}

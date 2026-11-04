package com.securevault.securevault.service;

import com.securevault.securevault.common.MessageKeys;
import com.securevault.securevault.entity.Document;
import com.securevault.securevault.entity.DocumentShare;
import com.securevault.securevault.util.PaginationUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.securevault.securevault.entity.User;
import com.securevault.securevault.exception.DocumentNotFoundException;
import com.securevault.securevault.repository.DocumentRepository;
import com.securevault.securevault.repository.DocumentShareRepository;
import com.securevault.securevault.repository.UserRepository;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

  private final DocumentRepository documentRepository;
  private final DocumentShareRepository documentShareRepository;
  private final UserRepository userRepository;
  private final DocumentValidationService validationService;
  private final FileStorageService fileStorageService;
  private final MessageSource messageSource;

  public DocumentService(
      DocumentRepository documentRepository,
      DocumentShareRepository documentShareRepository,
      UserRepository userRepository,
      DocumentValidationService validationService,
      FileStorageService fileStorageService,
      MessageSource messageSource) {
    this.documentRepository = documentRepository;
    this.documentShareRepository = documentShareRepository;
    this.userRepository = userRepository;
    this.validationService = validationService;
    this.fileStorageService = fileStorageService;
    this.messageSource = messageSource;
  }

  @Transactional
  public Document uploadDocument(String email, MultipartFile file) throws IOException {
    validationService.validate(file);

    User owner = userRepository
        .findByEmail(email)
        .orElseThrow(
            () -> new IllegalArgumentException(
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

    User user = getAuthenticatedUser(email);

    Document document = getActiveDocument(documentId);

    boolean isOwner = document.getOwner().getId().equals(user.getId());

    if (isOwner) {
      return document;
    }

    DocumentShare share = documentShareRepository
        .findActiveShare(documentId, user.getId(), OffsetDateTime.now())
        .orElseThrow(
            () -> new DocumentNotFoundException(getMessage(MessageKeys.DOCUMENT_NOT_FOUND)));

    if (!share.isCanView() || !share.isCanDownload()) {
      throw new DocumentNotFoundException(getMessage(MessageKeys.DOCUMENT_NOT_FOUND));
    }

    return document;
  }

  @Transactional(readOnly = true)
  public Document getDocumentForPreview(String email, UUID documentId) {

    User user = getAuthenticatedUser(email);

    Document document = getActiveDocument(documentId);

    boolean isOwner = document.getOwner().getId().equals(user.getId());

    if (!isOwner) {
      DocumentShare share = documentShareRepository
          .findActiveShare(documentId, user.getId(), OffsetDateTime.now())
          .orElseThrow(
              () -> new DocumentNotFoundException(getMessage(MessageKeys.DOCUMENT_NOT_FOUND)));

      if (!share.isCanView()) {
        throw new DocumentNotFoundException(getMessage(MessageKeys.DOCUMENT_NOT_FOUND));
      }
    }

    if (!validationService.isPreviewSupported(document.getMimeType())) {
      throw new IllegalArgumentException(getMessage(MessageKeys.DOCUMENT_PREVIEW_UNSUPPORTED));
    }

    return document;
  }

  @Transactional(readOnly = true)
  public Page<Document> listDocuments(
      String email,
      String search,
      String mimeType,
      String sortBy,
      String direction,
      Integer page,
      Integer size) {

    User owner = getAuthenticatedUser(email);

    String normalizedSearch = search == null ? "" : search.trim();
    String normalizedMimeType = mimeType == null ? "" : mimeType.trim();

    String normalizedSortBy = sortBy == null || sortBy.isBlank() ? "createdAt" : sortBy.trim();

    if (!normalizedSortBy.equals("fileName")
        && !normalizedSortBy.equals("createdAt")) {
      throw new IllegalArgumentException(
          getMessage(MessageKeys.DOCUMENT_SORT_INVALID_FIELD));
    }

    Pageable pageable = PaginationUtil.createPageable(
        page,
        size,
        normalizedSortBy,
        direction,
        "createdAt",
        messageSource);

    boolean hasSearch = !normalizedSearch.isEmpty();
    boolean hasMimeType = !normalizedMimeType.isEmpty();

    if (hasSearch && hasMimeType) {
      return documentRepository
          .findByOwner_IdAndStatusAndFileNameContainingIgnoreCaseAndMimeType(
              owner.getId(),
              "ACTIVE",
              normalizedSearch,
              normalizedMimeType,
              pageable);
    }

    if (hasSearch) {
      return documentRepository
          .findByOwner_IdAndStatusAndFileNameContainingIgnoreCase(
              owner.getId(),
              "ACTIVE",
              normalizedSearch,
              pageable);
    }

    if (hasMimeType) {
      return documentRepository
          .findByOwner_IdAndStatusAndMimeType(
              owner.getId(),
              "ACTIVE",
              normalizedMimeType,
              pageable);
    }

    return documentRepository.findByOwner_IdAndStatus(
        owner.getId(), "ACTIVE", pageable);
  }

  @Transactional
  public Document renameDocument(String email, UUID documentId, String newFileName) {

    User owner = getAuthenticatedUser(email);

    Document document = documentRepository
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

    User owner = getAuthenticatedUser(email);

    Document document = documentRepository
        .findByIdAndOwner_IdAndStatus(documentId, owner.getId(), "ACTIVE")
        .orElseThrow(
            () -> new DocumentNotFoundException(getMessage(MessageKeys.DOCUMENT_NOT_FOUND)));

    fileStorageService.deleteFile(document.getStorageKey());

    document.setStatus("DELETED");

    documentRepository.save(document);
  }

  private User getAuthenticatedUser(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(
            () -> new IllegalArgumentException(getMessage(MessageKeys.USER_AUTHENTICATED_NOT_FOUND)));
  }

  private Document getActiveDocument(UUID documentId) {
    return documentRepository
        .findById(documentId)
        .filter(document -> "ACTIVE".equals(document.getStatus()))
        .orElseThrow(
            () -> new DocumentNotFoundException(getMessage(MessageKeys.DOCUMENT_NOT_FOUND)));
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

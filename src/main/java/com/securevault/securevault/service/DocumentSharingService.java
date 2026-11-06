package com.securevault.securevault.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securevault.securevault.common.MessageKeys;
import com.securevault.securevault.dto.ShareDocumentRequest;
import com.securevault.securevault.dto.ShareDocumentResponse;
import com.securevault.securevault.entity.Document;
import com.securevault.securevault.entity.DocumentShare;
import com.securevault.securevault.entity.User;
import com.securevault.securevault.exception.DocumentNotFoundException;
import com.securevault.securevault.repository.DocumentRepository;
import com.securevault.securevault.repository.DocumentShareRepository;
import com.securevault.securevault.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.securevault.securevault.dto.UpdateSharePermissionRequest;

@Service
public class DocumentSharingService {

  private static final Logger logger = LoggerFactory.getLogger(DocumentSharingService.class);

  private final DocumentRepository documentRepository;
  private final DocumentShareRepository documentShareRepository;
  private final UserRepository userRepository;
  private final AuditLogRecorder auditLogRecorder;
  private final ObjectMapper objectMapper;
  private final MessageSource messageSource;

  public DocumentSharingService(
      DocumentRepository documentRepository,
      DocumentShareRepository documentShareRepository,
      UserRepository userRepository,
      AuditLogRecorder auditLogRecorder,
      ObjectMapper objectMapper,
      MessageSource messageSource) {
    this.documentRepository = documentRepository;
    this.documentShareRepository = documentShareRepository;
    this.userRepository = userRepository;
    this.auditLogRecorder = auditLogRecorder;
    this.objectMapper = objectMapper;
    this.messageSource = messageSource;
  }

  @Transactional
  public ShareDocumentResponse shareDocument(
      String email, UUID documentId, ShareDocumentRequest request) {

    User actor = userRepository
        .findByEmail(email)
        .orElseThrow(
            () -> new IllegalArgumentException(
                getMessage(MessageKeys.USER_AUTHENTICATED_NOT_FOUND)));

    Document document = documentRepository
        .findById(documentId)
        .filter(doc -> "ACTIVE".equals(doc.getStatus()))
        .orElseThrow(
            () -> new DocumentNotFoundException(getMessage(MessageKeys.DOCUMENT_NOT_FOUND)));

    boolean isOwner = document.getOwner().getId().equals(actor.getId());

    DocumentShare existingPermission = null;

    if (!isOwner) {
      existingPermission = documentShareRepository
          .findActiveShare(documentId, actor.getId(), OffsetDateTime.now())
          .orElseThrow(
              () -> new DocumentNotFoundException(getMessage(MessageKeys.DOCUMENT_NOT_FOUND)));

      if (!existingPermission.isCanShare()) {
        recordShareFailureAudit(actor, documentId, "USER_NOT_ALLOWED_TO_SHARE");

        throw new IllegalArgumentException(
            getMessage(MessageKeys.DOCUMENT_SHARE_PERMISSION_DENIED));
      }
    }

    User recipient = userRepository
        .findByEmail(request.getEmail().trim().toLowerCase(Locale.ROOT))
        .orElseThrow(
            () -> new IllegalArgumentException(
                getMessage(MessageKeys.DOCUMENT_SHARE_RECIPIENT_NOT_FOUND)));

    if (recipient.getId().equals(actor.getId())) {
      throw new IllegalArgumentException(getMessage(MessageKeys.DOCUMENT_SHARE_SELF_NOT_ALLOWED));
    }

    OffsetDateTime now = OffsetDateTime.now();

    if (request.getExpiresAt() != null && !request.getExpiresAt().isAfter(now)) {
      throw new IllegalArgumentException(getMessage(MessageKeys.DOCUMENT_SHARE_EXPIRY_INVALID));
    }

    if (request.isCanDownload() && !request.isCanView()) {
      throw new IllegalArgumentException(getMessage(MessageKeys.DOCUMENT_SHARE_PERMISSION_DENIED));
    }

    if (request.isCanShare() && !request.isCanView()) {
      throw new IllegalArgumentException(getMessage(MessageKeys.DOCUMENT_SHARE_PERMISSION_DENIED));
    }

    if (!isOwner
        && (request.isCanView() && !existingPermission.isCanView()
            || request.isCanDownload() && !existingPermission.isCanDownload()
            || request.isCanShare() && !existingPermission.isCanShare())) {

      recordShareFailureAudit(actor, documentId, "REQUESTED_PERMISSION_EXCEEDS_GRANTED_PERMISSION");

      throw new IllegalArgumentException(getMessage(MessageKeys.DOCUMENT_SHARE_PERMISSION_DENIED));
    }

    boolean alreadyShared = documentShareRepository.findActiveShare(documentId, recipient.getId(), now).isPresent();

    if (alreadyShared) {
      throw new IllegalArgumentException(getMessage(MessageKeys.DOCUMENT_SHARE_ALREADY_EXISTS));
    }

    DocumentShare share = new DocumentShare();
    share.setDocument(document);
    share.setSharedBy(actor);
    share.setSharedWith(recipient);
    share.setCanView(request.isCanView());
    share.setCanDownload(request.isCanDownload());
    share.setCanShare(request.isCanShare());
    share.setExpiresAt(request.getExpiresAt());

    DocumentShare savedShare = documentShareRepository.save(share);

    recordShareAudit(actor, document, recipient, savedShare);

    return ShareDocumentResponse.from(savedShare);
  }

  @Transactional(readOnly = true)
  public List<ShareDocumentResponse> getSharedDocuments(String email) {

    User user = userRepository
        .findByEmail(email)
        .orElseThrow(
            () -> new IllegalArgumentException(
                getMessage(MessageKeys.USER_AUTHENTICATED_NOT_FOUND)));

    List<DocumentShare> activeShares = documentShareRepository.findActiveSharesForUser(user.getId(),
        OffsetDateTime.now());

    return activeShares.stream().map(ShareDocumentResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public List<ShareDocumentResponse> getSharedByMe(String email) {

    User user = userRepository
        .findByEmail(email)
        .orElseThrow(
            () -> new IllegalArgumentException(
                getMessage(MessageKeys.USER_AUTHENTICATED_NOT_FOUND)));

    List<DocumentShare> activeShares = documentShareRepository.findActiveSharesByOwner(
        user.getId(), OffsetDateTime.now());

    return activeShares.stream().map(ShareDocumentResponse::from).toList();
  }

  @Transactional
  public ShareDocumentResponse updateSharePermission(
      String email,
      UUID documentId,
      UUID shareId,
      UpdateSharePermissionRequest request) {

    User owner = userRepository
        .findByEmail(email)
        .orElseThrow(
            () -> new IllegalArgumentException(
                getMessage(MessageKeys.USER_AUTHENTICATED_NOT_FOUND)));

    DocumentShare share = documentShareRepository
        .findShareOwnedBy(shareId, documentId, owner.getId())
        .orElseThrow(
            () -> new DocumentNotFoundException(
                getMessage(MessageKeys.DOCUMENT_SHARE_NOT_FOUND)));

    if (share.getExpiresAt() != null
        && !share.getExpiresAt().isAfter(OffsetDateTime.now())) {
      throw new DocumentNotFoundException(
          getMessage(MessageKeys.DOCUMENT_SHARE_NOT_FOUND));
    }

    // Update each permission independently.
    share.setCanView(request.isCanView());
    share.setCanDownload(request.isCanDownload());
    share.setCanShare(request.isCanShare());

    DocumentShare updatedShare = documentShareRepository.save(share);

    recordPermissionUpdateAudit(owner, updatedShare);

    return ShareDocumentResponse.from(updatedShare);
  }

  @Transactional
  public void revokeShare(String email, UUID documentId, UUID shareId) {

    User owner = userRepository
        .findByEmail(email)
        .orElseThrow(
            () -> new IllegalArgumentException(
                getMessage(MessageKeys.USER_AUTHENTICATED_NOT_FOUND)));

    DocumentShare share = documentShareRepository
        .findShareOwnedBy(shareId, documentId, owner.getId())
        .orElseThrow(
            () -> new DocumentNotFoundException(
                getMessage(MessageKeys.DOCUMENT_SHARE_NOT_FOUND)));

    share.setRevokedAt(OffsetDateTime.now());

    documentShareRepository.save(share);

    recordRevokeAudit(owner, share);
  }

  private void recordShareFailureAudit(User actor, UUID documentId, String reason) {
    try {
      auditLogRecorder.record(
          actor.getId(),
          "DOCUMENT_SHARE_FAILED",
          "DOCUMENT",
          documentId.toString(),
          "DENIED",
          null,
          objectMapper.valueToTree(Map.of("reason", reason)));
    } catch (RuntimeException exception) {
      logger.error("Failed to persist a document sharing failure audit event.", exception);
    }
  }

  private void recordShareAudit(
      User actor, Document document, User recipient, DocumentShare share) {
    try {
      auditLogRecorder.record(
          actor.getId(),
          "DOCUMENT_SHARED",
          "DOCUMENT",
          document.getId().toString(),
          "SUCCESS",
          null,
          objectMapper.valueToTree(
              Map.of(
                  "recipientEmail", recipient.getEmail(),
                  "shareId", share.getId().toString(),
                  "canView", share.isCanView(),
                  "canDownload", share.isCanDownload(),
                  "canShare", share.isCanShare())));
    } catch (RuntimeException exception) {
      logger.error("Failed to persist a document sharing audit event.", exception);
    }
  }

  private void recordPermissionUpdateAudit(User actor, DocumentShare share) {
    try {
      auditLogRecorder.record(
          actor.getId(),
          "DOCUMENT_SHARE_PERMISSION_UPDATED",
          "DOCUMENT",
          share.getDocument().getId().toString(),
          "SUCCESS",
          null,
          objectMapper.valueToTree(
              Map.of(
                  "shareId", share.getId().toString(),
                  "recipientEmail", share.getSharedWith().getEmail(),
                  "canView", share.isCanView(),
                  "canDownload", share.isCanDownload(),
                  "canShare", share.isCanShare())));
    } catch (RuntimeException exception) {
      logger.error("Failed to persist document share permission update audit event.", exception);
    }
  }

  private void recordRevokeAudit(User actor, DocumentShare share) {
    try {
      auditLogRecorder.record(
          actor.getId(),
          "DOCUMENT_SHARE_REVOKED",
          "DOCUMENT",
          share.getDocument().getId().toString(),
          "SUCCESS",
          null,
          objectMapper.valueToTree(
              Map.of(
                  "shareId", share.getId().toString(),
                  "recipientEmail", share.getSharedWith().getEmail())));
    } catch (RuntimeException exception) {
      logger.error("Failed to persist a document share revocation audit event.", exception);
    }
  }

  private String getMessage(String messageKey) {
    return messageSource.getMessage(messageKey, null, Locale.getDefault());
  }
}

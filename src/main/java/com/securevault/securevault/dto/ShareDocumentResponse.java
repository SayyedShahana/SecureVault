package com.securevault.securevault.dto;

import com.securevault.securevault.entity.DocumentShare;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ShareDocumentResponse(
    UUID shareId,
    UUID documentId,
    String documentName,
    String sharedWithEmail,
    boolean canView,
    boolean canDownload,
    boolean canShare,
    OffsetDateTime expiresAt,
    OffsetDateTime createdAt) {

  public static ShareDocumentResponse from(DocumentShare share) {
    return new ShareDocumentResponse(
        share.getId(),
        share.getDocument().getId(),
        share.getDocument().getFileName(),
        share.getSharedWith().getEmail(),
        share.isCanView(),
        share.isCanDownload(),
        share.isCanShare(),
        share.getExpiresAt(),
        share.getCreatedAt());
  }
}

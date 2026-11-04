package com.securevault.securevault.dto;

import com.securevault.securevault.entity.Document;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentUploadResponse(
    UUID documentId,
    String fileName,
    String mimeType,
    long fileSizeBytes,
    String status,
    OffsetDateTime createdAt) {

  public static DocumentUploadResponse from(Document document) {
    return new DocumentUploadResponse(
        document.getId(),
        document.getFileName(),
        document.getMimeType(),
        document.getFileSizeBytes(),
        document.getStatus(),
        document.getCreatedAt());
  }
}

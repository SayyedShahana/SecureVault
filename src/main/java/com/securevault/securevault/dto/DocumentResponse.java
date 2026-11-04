package com.securevault.securevault.dto;

import com.securevault.securevault.entity.Document;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentResponse(
    UUID documentId,
    String fileName,
    String mimeType,
    long fileSizeBytes,
    String status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  public static DocumentResponse from(Document document) {
    return new DocumentResponse(
        document.getId(),
        document.getFileName(),
        document.getMimeType(),
        document.getFileSizeBytes(),
        document.getStatus(),
        document.getCreatedAt(),
        document.getUpdatedAt());
  }
}

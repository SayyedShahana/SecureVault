package com.securevault.securevault.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentResponse(
    UUID documentId,
    String fileName,
    String mimeType,
    long fileSizeBytes,
    String status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}

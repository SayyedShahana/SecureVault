package com.securevault.securevault.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securevault.securevault.entity.AuditLog;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
    Long id,
    Long actorUserId,
    String action,
    String resourceType,
    String resourceId,
    String outcome,
    String ipAddress,
    UUID requestId,
    Map<String, Object> details,
    OffsetDateTime createdAt) {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static AuditLogResponse from(AuditLog auditLog) {
    return new AuditLogResponse(
        auditLog.getId(),
        auditLog.getActorUserId(),
        auditLog.getAction(),
        auditLog.getResourceType(),
        auditLog.getResourceId(),
        auditLog.getOutcome(),
        auditLog.getIpAddress(),
        auditLog.getRequestId(),
        convertDetails(auditLog.getDetails()),
        auditLog.getCreatedAt());
  }

  private static Map<String, Object> convertDetails(JsonNode details) {
    if (details == null || details.isNull()) {
      return null;
    }

    return OBJECT_MAPPER.convertValue(details, new TypeReference<Map<String, Object>>() {});
  }
}

package com.securevault.securevault.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.securevault.securevault.entity.AuditLog;
import com.securevault.securevault.repository.AuditLogRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogRecorder {

  private final AuditLogRepository auditLogRepository;

  public AuditLogRecorder(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(
      Long actorUserId,
      String action,
      String resourceType,
      String resourceId,
      String outcome,
      UUID requestId,
      JsonNode details) {

    AuditLog auditLog = new AuditLog();
    auditLog.setActorUserId(actorUserId);
    auditLog.setAction(action);
    auditLog.setResourceType(resourceType);
    auditLog.setResourceId(resourceId);
    auditLog.setOutcome(outcome);
    auditLog.setRequestId(requestId);
    auditLog.setDetails(details);

    auditLogRepository.save(auditLog);
  }
}

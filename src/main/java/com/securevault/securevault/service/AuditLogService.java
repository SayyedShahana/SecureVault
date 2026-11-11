
package com.securevault.securevault.service;

import com.securevault.securevault.entity.AuditLog;
import com.securevault.securevault.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class AuditLogService {

  private final AuditLogRepository auditLogRepository;

  public AuditLogService(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @Transactional(readOnly = true)
  public Page<AuditLog> getAllAuditLogs(int page, int size) {

    Pageable pageable = PageRequest.of(
        page,
        size,
        Sort.by(Sort.Direction.DESC, "createdAt"));

    return auditLogRepository.findAll(pageable);
  }

  @Transactional(readOnly = true)
  public Page<AuditLog> getMyAuditLogs(Long actorUserId, int page, int size) {

    Pageable pageable = PageRequest.of(
        page,
        size,
        Sort.by(Sort.Direction.DESC, "createdAt"));

    return auditLogRepository.findByActorUserIdAndResourceTypeNot(
        actorUserId,
        "AUTHENTICATION",
        pageable);
  }

  @Transactional(readOnly = true)
  public Map<String, Long> getAuditLogSummary() {

    long successfulEvents = auditLogRepository.countByOutcomeIgnoreCase("SUCCESS");

    long failedEvents = auditLogRepository.countByOutcomeIgnoreCase("FAILURE");

    long totalAuditRecords = auditLogRepository.count();

    return Map.of(
        "successfulEvents", successfulEvents,
        "failedEvents", failedEvents,
        "totalAuditRecords", totalAuditRecords);
  }
}

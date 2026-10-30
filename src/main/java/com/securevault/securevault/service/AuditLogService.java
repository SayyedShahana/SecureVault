package com.securevault.securevault.service;

import com.securevault.securevault.entity.AuditLog;
import com.securevault.securevault.repository.AuditLogRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

  private final AuditLogRepository auditLogRepository;

  public AuditLogService(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @Transactional(readOnly = true)
  public List<AuditLog> getAllAuditLogs() {
    return auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
  }
}

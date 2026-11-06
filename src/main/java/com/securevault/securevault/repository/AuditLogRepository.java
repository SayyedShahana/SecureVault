package com.securevault.securevault.repository;

import com.securevault.securevault.entity.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

  List<AuditLog> findByActorUserIdOrderByCreatedAtDesc(Long actorUserId);
}

package com.securevault.securevault.repository;

import com.securevault.securevault.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

  Page<AuditLog> findByActorUserIdAndResourceTypeNot(
      Long actorUserId,
      String resourceType,
      Pageable pageable);

  long countByOutcomeIgnoreCase(String outcome);
}

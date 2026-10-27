package com.securevault.securevault.repository;

import com.securevault.securevault.entity.LoginAttempt;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

  long countByEmailAndSuccessfulFalseAndAttemptedAtAfter(String email, OffsetDateTime time);

  void deleteByEmailAndSuccessfulFalse(String email);
}

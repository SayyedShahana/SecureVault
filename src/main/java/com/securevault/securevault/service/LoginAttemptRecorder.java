package com.securevault.securevault.service;

import com.securevault.securevault.entity.LoginAttempt;
import com.securevault.securevault.repository.LoginAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptRecorder {

  private final LoginAttemptRepository repository;

  public LoginAttemptRecorder(LoginAttemptRepository repository) {
    this.repository = repository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(String email, boolean successful) {
    repository.save(new LoginAttempt(email, successful));
  }
}

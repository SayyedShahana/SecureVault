package com.securevault.securevault.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "login_attempts")
public class LoginAttempt {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "attempt_id")
  private Long id;

  @Column(name = "email_attempted", nullable = false, length = 255)
  private String email;

  @Column(name = "success", nullable = false)
  private boolean successful;

  @Column(name = "attempted_at", nullable = false, updatable = false)
  private OffsetDateTime attemptedAt;

  // Existing database columns
  @Column(name = "user_id")
  private Long userId;

  @Column(name = "ip_address", columnDefinition = "inet", insertable = false, updatable = false)
  private String ipAddress;

  @Column(name = "failure_reason")
  private String failureReason;

  public LoginAttempt() {}

  public LoginAttempt(String email, boolean successful) {
    this.email = email;
    this.successful = successful;
  }

  @PrePersist
  protected void onCreate() {
    if (attemptedAt == null) {
      attemptedAt = OffsetDateTime.now();
    }
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public boolean isSuccessful() {
    return successful;
  }

  public void setSuccessful(boolean successful) {
    this.successful = successful;
  }

  public OffsetDateTime getAttemptedAt() {
    return attemptedAt;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public String getFailureReason() {
    return failureReason;
  }

  public void setFailureReason(String failureReason) {
    this.failureReason = failureReason;
  }
}

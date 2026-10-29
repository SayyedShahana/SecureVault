
package com.securevault.securevault.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securevault.securevault.dto.LoginRequest;
import com.securevault.securevault.entity.User;
import com.securevault.securevault.entity.UserSession;
import com.securevault.securevault.repository.LoginAttemptRepository;
import com.securevault.securevault.repository.UserRepository;
import com.securevault.securevault.repository.UserSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private static final int REFRESH_TOKEN_BYTES = 32;
  private static final long REFRESH_TOKEN_DAYS = 7;
  private static final int MAX_FAILED_ATTEMPTS = 5;
  private static final long LOCK_WINDOW_MINUTES = 15;

  private final UserRepository userRepository;
  private final UserSessionRepository userSessionRepository;
  private final LoginAttemptRepository loginAttemptRepository;
  private final LoginAttemptRecorder loginAttemptRecorder;
  private final AuditLogRecorder auditLogRecorder;
  private final ObjectMapper objectMapper;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final SecureRandom secureRandom = new SecureRandom();

  public AuthService(
      UserRepository userRepository,
      UserSessionRepository userSessionRepository,
      LoginAttemptRepository loginAttemptRepository,
      LoginAttemptRecorder loginAttemptRecorder,
      AuditLogRecorder auditLogRecorder,
      ObjectMapper objectMapper,
      PasswordEncoder passwordEncoder,
      JwtService jwtService) {

    this.userRepository = userRepository;
    this.userSessionRepository = userSessionRepository;
    this.loginAttemptRepository = loginAttemptRepository;
    this.loginAttemptRecorder = loginAttemptRecorder;
    this.auditLogRecorder = auditLogRecorder;
    this.objectMapper = objectMapper;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  @Transactional
  public LoginTokens login(LoginRequest request) {

    String email = request.getEmail().trim().toLowerCase(java.util.Locale.ROOT);

    OffsetDateTime windowStart = OffsetDateTime.now().minusMinutes(LOCK_WINDOW_MINUTES);

    long failedAttempts = loginAttemptRepository.countByEmailAndSuccessfulFalseAndAttemptedAtAfter(
        email, windowStart);

    if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
      throw new IllegalArgumentException(
          "Too many failed login attempts. Try again after 15 minutes.");
    }

    User user = userRepository.findByEmail(email).orElse(null);

    if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {

      loginAttemptRecorder.record(email, false);

      recordAudit(
          user != null ? user.getId() : null,
          "LOGIN",
          "FAILURE",
          email);

      throw new IllegalArgumentException("Invalid email or password");
    }

    loginAttemptRecorder.record(email, true);

    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = generateRefreshToken();

    UserSession session = new UserSession();
    session.setUser(user);
    session.setRefreshTokenHash(hashToken(refreshToken));
    session.setExpiresAt(OffsetDateTime.now().plusDays(REFRESH_TOKEN_DAYS));
    session.setLastUsedAt(OffsetDateTime.now());

    userSessionRepository.save(session);

    recordAudit(user.getId(), "LOGIN", "SUCCESS", email);

    return new LoginTokens(accessToken, refreshToken);
  }

  @Transactional
  public LoginTokens refresh(String refreshToken) {

    if (refreshToken == null || refreshToken.isBlank()) {
      throw new IllegalArgumentException("Refresh token is required");
    }

    String tokenHash = hashToken(refreshToken);

    UserSession session = userSessionRepository
        .findByRefreshTokenHash(tokenHash)
        .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

    if (session.getRevokedAt() != null || !session.getExpiresAt().isAfter(OffsetDateTime.now())) {
      throw new IllegalArgumentException("Refresh token has expired or been revoked");
    }

    User user = session.getUser();

    String newAccessToken = jwtService.generateAccessToken(user);
    String newRefreshToken = generateRefreshToken();

    session.setRefreshTokenHash(hashToken(newRefreshToken));
    session.setExpiresAt(OffsetDateTime.now().plusDays(REFRESH_TOKEN_DAYS));
    session.setLastUsedAt(OffsetDateTime.now());

    userSessionRepository.save(session);

    return new LoginTokens(newAccessToken, newRefreshToken);
  }

  @Transactional
  public void logout(String refreshToken) {

    if (refreshToken == null || refreshToken.isBlank()) {
      throw new IllegalArgumentException("Refresh token is required");
    }

    String tokenHash = hashToken(refreshToken);

    UserSession session = userSessionRepository
        .findByRefreshTokenHash(tokenHash)
        .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

    if (session.getRevokedAt() == null) {
      session.setRevokedAt(OffsetDateTime.now());
      userSessionRepository.save(session);
    }
  }

  private void recordAudit(Long userId, String action, String outcome, String email) {

    try {
      auditLogRecorder.record(
          userId,
          action,
          "AUTHENTICATION",
          null,
          outcome,
          null,
          objectMapper.valueToTree(Map.of("email", email)));
    } catch (RuntimeException ex) {
      // Audit failures must be monitored; do not expose internal details to the
      // client.
      System.err.println("Failed to persist an authentication audit event.");
    }
  }

  private String generateRefreshToken() {
    byte[] randomBytes = new byte[REFRESH_TOKEN_BYTES];
    secureRandom.nextBytes(randomBytes);

    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
  }

  private String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

      return Base64.getEncoder().encodeToString(hash);

    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available", ex);
    }
  }

  public record LoginTokens(String accessToken, String refreshToken) {
  }
}


package com.securevault.securevault.controller;

import com.securevault.securevault.common.MessageKeys;
import com.securevault.securevault.dto.ApiResponse;
import com.securevault.securevault.dto.AuditLogResponse;
import com.securevault.securevault.dto.UserResponse;
import com.securevault.securevault.repository.UserRepository;
import com.securevault.securevault.service.AuditLogService;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private final UserRepository userRepository;
  private final AuditLogService auditLogService;
  private final MessageSource messageSource;

  public AdminController(
      UserRepository userRepository,
      AuditLogService auditLogService,
      MessageSource messageSource) {
    this.userRepository = userRepository;
    this.auditLogService = auditLogService;
    this.messageSource = messageSource;
  }

  @GetMapping("/users")
  public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {

    List<UserResponse> users = userRepository.findAll()
        .stream()
        .map(UserResponse::from)
        .toList();

    String message = messageSource.getMessage(
        MessageKeys.ADMIN_USERS_SUCCESS, null, Locale.getDefault());

    return ResponseEntity.ok(new ApiResponse<>(true, message, users));
  }

  @GetMapping("/audit-logs")
  public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAllAuditLogs(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    if (page < 0 || size < 1 || size > 100) {
      throw new IllegalArgumentException("Invalid pagination parameters.");
    }

    Page<AuditLogResponse> auditLogs = auditLogService
        .getAllAuditLogs(page, size)
        .map(AuditLogResponse::from);

    String message = messageSource.getMessage(
        MessageKeys.ADMIN_AUDIT_LOGS_SUCCESS, null, Locale.getDefault());

    return ResponseEntity.ok(
        new ApiResponse<>(true, message, auditLogs));
  }

  @GetMapping("/audit-logs/summary")
  public ResponseEntity<ApiResponse<Map<String, Long>>> getAuditLogSummary() {

    Map<String, Long> summary = auditLogService.getAuditLogSummary();

    String message = messageSource.getMessage(
        MessageKeys.ADMIN_AUDIT_LOGS_SUCCESS,
        null,
        Locale.getDefault());

    return ResponseEntity.ok(
        new ApiResponse<>(true, message, summary));
  }
}

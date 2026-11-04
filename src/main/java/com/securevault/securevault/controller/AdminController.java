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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private final UserRepository userRepository;
  private final AuditLogService auditLogService;
  private final MessageSource messageSource;

  public AdminController(
      UserRepository userRepository, AuditLogService auditLogService, MessageSource messageSource) {
    this.userRepository = userRepository;
    this.auditLogService = auditLogService;
    this.messageSource = messageSource;
  }

  @GetMapping("/users")
  public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {

    List<UserResponse> users = userRepository.findAll().stream().map(UserResponse::from).toList();

    String message =
        messageSource.getMessage(MessageKeys.ADMIN_USERS_SUCCESS, null, Locale.getDefault());

    ApiResponse<List<UserResponse>> response = new ApiResponse<>(true, message, users);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/audit-logs")
  public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAllAuditLogs() {

    List<AuditLogResponse> auditLogs =
        auditLogService.getAllAuditLogs().stream().map(AuditLogResponse::from).toList();

    String message =
        messageSource.getMessage(MessageKeys.ADMIN_AUDIT_LOGS_SUCCESS, null, Locale.getDefault());

    ApiResponse<List<AuditLogResponse>> response = new ApiResponse<>(true, message, auditLogs);

    return ResponseEntity.ok(response);
  }
}

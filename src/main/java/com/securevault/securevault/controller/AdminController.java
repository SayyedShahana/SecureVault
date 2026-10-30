package com.securevault.securevault.controller;

import com.securevault.securevault.dto.ApiResponse;
import com.securevault.securevault.dto.AuditLogResponse;
import com.securevault.securevault.dto.UserResponse;
import com.securevault.securevault.repository.UserRepository;
import com.securevault.securevault.service.AuditLogService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private final UserRepository userRepository;
  private final AuditLogService auditLogService;

  public AdminController(UserRepository userRepository, AuditLogService auditLogService) {
    this.userRepository = userRepository;
    this.auditLogService = auditLogService;
  }

  @GetMapping("/users")
  public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {

    List<UserResponse> users = userRepository.findAll().stream().map(UserResponse::from).toList();

    ApiResponse<List<UserResponse>> response =
        new ApiResponse<>(true, "Users retrieved successfully", users);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/audit-logs")
  public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAllAuditLogs() {

    List<AuditLogResponse> auditLogs =
        auditLogService.getAllAuditLogs().stream().map(AuditLogResponse::from).toList();

    ApiResponse<List<AuditLogResponse>> response =
        new ApiResponse<>(true, "Audit logs retrieved successfully", auditLogs);

    return ResponseEntity.ok(response);
  }
}

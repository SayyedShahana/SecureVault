package com.securevault.securevault.controller;

import com.securevault.securevault.common.MessageKeys;
import com.securevault.securevault.dto.ApiResponse;
import com.securevault.securevault.dto.AuditLogResponse;
import com.securevault.securevault.entity.User;
import com.securevault.securevault.repository.UserRepository;
import com.securevault.securevault.service.AuditLogService;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.securevault.securevault.exception.DocumentNotFoundException;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final MessageSource messageSource;

    public AuditLogController(
            UserRepository userRepository,
            AuditLogService auditLogService,
            MessageSource messageSource) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.messageSource = messageSource;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getMyAuditLogs(
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(
                        () -> new DocumentNotFoundException(
                                messageSource.getMessage(
                                        MessageKeys.USER_AUTHENTICATED_NOT_FOUND,
                                        null,
                                        Locale.getDefault())));
        List<AuditLogResponse> auditLogs = auditLogService.getMyAuditLogs(user.getId()).stream()
                .map(AuditLogResponse::from)
                .toList();

        String message = messageSource.getMessage(
                MessageKeys.USER_AUDIT_LOGS_SUCCESS, null, Locale.getDefault());

        ApiResponse<List<AuditLogResponse>> response = new ApiResponse<>(true, message, auditLogs);

        return ResponseEntity.ok(response);
    }
}
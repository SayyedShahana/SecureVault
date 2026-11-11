
package com.securevault.securevault.controller;

import com.securevault.securevault.common.MessageKeys;
import com.securevault.securevault.dto.ApiResponse;
import com.securevault.securevault.dto.AuditLogResponse;
import com.securevault.securevault.entity.User;
import com.securevault.securevault.exception.DocumentNotFoundException;
import com.securevault.securevault.repository.UserRepository;
import com.securevault.securevault.service.AuditLogService;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getMyAuditLogs(
                        Authentication authentication,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                if (page < 0 || size < 1 || size > 100) {
                        throw new IllegalArgumentException("Invalid pagination parameters.");
                }

                User user = userRepository.findByEmail(authentication.getName())
                                .orElseThrow(
                                                () -> new DocumentNotFoundException(
                                                                messageSource.getMessage(
                                                                                MessageKeys.USER_AUTHENTICATED_NOT_FOUND,
                                                                                null,
                                                                                Locale.getDefault())));

                Page<AuditLogResponse> auditLogs = auditLogService
                                .getMyAuditLogs(user.getId(), page, size)
                                .map(AuditLogResponse::from);

                String message = messageSource.getMessage(
                                MessageKeys.USER_AUDIT_LOGS_SUCCESS,
                                null,
                                Locale.getDefault());

                return ResponseEntity.ok(
                                new ApiResponse<>(true, message, auditLogs));
        }
}

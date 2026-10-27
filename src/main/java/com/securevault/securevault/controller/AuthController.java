package com.securevault.securevault.controller;

import com.securevault.securevault.common.JsonFields;
import com.securevault.securevault.common.MessageKeys;
import com.securevault.securevault.dto.ApiResponse;
import com.securevault.securevault.dto.LoginRequest;
import com.securevault.securevault.dto.RegisterRequest;
import com.securevault.securevault.dto.UserResponse;
import com.securevault.securevault.entity.User;
import com.securevault.securevault.service.AuthService;
import com.securevault.securevault.service.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final UserService userService;
  private final AuthService authService;
  private final MessageSource messageSource;

  public AuthController(
      UserService userService, AuthService authService, MessageSource messageSource) {
    this.userService = userService;
    this.authService = authService;
    this.messageSource = messageSource;
  }

  private String getMessage(String key) {
    return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
  }

  @PostMapping("/register")
  public ResponseEntity<ApiResponse<UserResponse>> register(
      @Valid @RequestBody RegisterRequest request) {

    User user = userService.register(request);

    UserResponse userResponse =
        new UserResponse(
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            user.getRole().getRoleName(),
            user.isEmailVerified());

    ApiResponse<UserResponse> response =
        new ApiResponse<>(true, getMessage(MessageKeys.AUTH_REGISTER_SUCCESS), userResponse);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<Map<String, String>>> login(
      @Valid @RequestBody LoginRequest request) {

    AuthService.LoginTokens tokens = authService.login(request);

    ApiResponse<Map<String, String>> response =
        new ApiResponse<>(
            true,
            getMessage(MessageKeys.AUTH_LOGIN_SUCCESS),
            Map.of(
                JsonFields.ACCESS_TOKEN, tokens.accessToken(),
                JsonFields.REFRESH_TOKEN, tokens.refreshToken()));

    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<Map<String, String>>> refresh(
      @RequestBody Map<String, String> request) {

    AuthService.LoginTokens tokens = authService.refresh(request.get(JsonFields.REFRESH_TOKEN));

    ApiResponse<Map<String, String>> response =
        new ApiResponse<>(
            true,
            getMessage(MessageKeys.AUTH_REFRESH_SUCCESS),
            Map.of(
                JsonFields.ACCESS_TOKEN, tokens.accessToken(),
                JsonFields.REFRESH_TOKEN, tokens.refreshToken()));

    return ResponseEntity.ok(response);
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<String>> logout(@RequestBody Map<String, String> request) {

    authService.logout(request.get(JsonFields.REFRESH_TOKEN));

    ApiResponse<String> response =
        new ApiResponse<>(
            true,
            getMessage(MessageKeys.AUTH_LOGOUT_SUCCESS),
            getMessage(MessageKeys.AUTH_LOGOUT_SESSION_REVOKED));

    return ResponseEntity.ok(response);
  }
}

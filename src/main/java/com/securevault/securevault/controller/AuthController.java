package com.securevault.securevault.controller;

import com.securevault.securevault.constant.ApiMessages;
import com.securevault.securevault.dto.ApiResponse;
import com.securevault.securevault.dto.LoginRequest;
import com.securevault.securevault.dto.RegisterRequest;
import com.securevault.securevault.dto.UserResponse;
import com.securevault.securevault.entity.User;
import com.securevault.securevault.service.AuthService;
import com.securevault.securevault.service.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final UserService userService;
  private final AuthService authService;

  public AuthController(UserService userService, AuthService authService) {
    this.userService = userService;
    this.authService = authService;
  }

  @PostMapping("/register")
  public ResponseEntity<ApiResponse<UserResponse>> register(
      @Valid @RequestBody RegisterRequest request) {

    User user = userService.register(request);

    UserResponse userResponse = toUserResponse(user);

    ApiResponse<UserResponse> response =
        new ApiResponse<>(true, ApiMessages.User.REGISTRATION_SUCCESS, userResponse);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  private UserResponse toUserResponse(User user) {
    return new UserResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        user.getRole().getRoleName(),
        user.isEmailVerified());
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<Map<String, String>>> login(
      @Valid @RequestBody LoginRequest request) {

    AuthService.LoginTokens tokens = authService.login(request);

    ApiResponse<Map<String, String>> response =
        new ApiResponse<>(
            true,
            "Login successful",
            Map.of(
                "accessToken", tokens.accessToken(),
                "refreshToken", tokens.refreshToken()));

    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<Map<String, String>>> refresh(
      @RequestBody Map<String, String> request) {

    AuthService.LoginTokens tokens = authService.refresh(request.get("refreshToken"));

    ApiResponse<Map<String, String>> response =
        new ApiResponse<>(
            true,
            "Token refreshed successfully",
            Map.of(
                "accessToken", tokens.accessToken(),
                "refreshToken", tokens.refreshToken()));

    return ResponseEntity.ok(response);
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<String>> logout(@RequestBody Map<String, String> request) {

    authService.logout(request.get("refreshToken"));

    ApiResponse<String> response = new ApiResponse<>(true, "Logout successful", "Session revoked");

    return ResponseEntity.ok(response);
  }
}

package com.securevault.securevault.controller;

import com.securevault.securevault.common.MessageKeys;
import com.securevault.securevault.dto.ApiResponse;
import com.securevault.securevault.dto.UserResponse;
import com.securevault.securevault.service.UserService;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;
  private final MessageSource messageSource;

  public UserController(UserService userService, MessageSource messageSource) {
    this.userService = userService;
    this.messageSource = messageSource;
  }

  @GetMapping("/{email}")
  public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(@PathVariable String email) {

    return userService
        .findByEmail(email)
        .map(
            user -> {
              UserResponse userResponse =
                  new UserResponse(
                      user.getId(),
                      user.getFullName(),
                      user.getEmail(),
                      user.getRole().getRoleName(),
                      user.isEmailVerified());

              String message =
                  messageSource.getMessage(MessageKeys.USER_FOUND, null, Locale.getDefault());

              return ResponseEntity.ok(new ApiResponse<>(true, message, userResponse));
            })
        .orElseGet(
            () -> {
              String message =
                  messageSource.getMessage(MessageKeys.USER_NOT_FOUND, null, Locale.getDefault());

              return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(new ApiResponse<>(false, message, null));
            });
  }
}

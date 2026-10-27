package com.securevault.securevault.controller;

import com.securevault.securevault.constant.ApiMessages;
import com.securevault.securevault.dto.ApiResponse;
import com.securevault.securevault.dto.RegisterRequest;
import com.securevault.securevault.dto.UserResponse;
import com.securevault.securevault.entity.User;
import com.securevault.securevault.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

        private final UserService userService;

        public AuthController(UserService userService) {
                this.userService = userService;
        }

        @PostMapping("/register")
        public ResponseEntity<ApiResponse<UserResponse>> register(
                        @Valid @RequestBody RegisterRequest request) {

                User user = userService.register(request);
                UserResponse userResponse = toUserResponse(user);

                ApiResponse<UserResponse> response = new ApiResponse<>(true, ApiMessages.User.REGISTRATION_SUCCESS,
                                userResponse);

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
}

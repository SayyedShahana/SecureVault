
package com.securevault.securevault.controller;

import com.securevault.securevault.dto.ApiResponse;
import com.securevault.securevault.dto.UserResponse;
import com.securevault.securevault.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{email}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(
            @PathVariable String email) {

        return userService.findByEmail(email)
                .map(user -> {
                    UserResponse userResponse = new UserResponse(
                            user.getId(),
                            user.getFullName(),
                            user.getEmail(),
                            user.getRole().getRoleName(),
                            user.isEmailVerified());
                    return ResponseEntity.ok(
                            new ApiResponse<>(
                                    true,
                                    "User found successfully",
                                    userResponse));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(
                                false,
                                "User not found",
                                null)));
    }
}

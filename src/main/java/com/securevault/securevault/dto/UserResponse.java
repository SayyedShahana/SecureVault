package com.securevault.securevault.dto;

import com.securevault.securevault.entity.User;

public class UserResponse {

  private Long id;
  private String fullName;
  private String email;
  private String role;
  private boolean emailVerified;

  public UserResponse() {}

  public UserResponse(Long id, String fullName, String email, String role, boolean emailVerified) {
    this.id = id;
    this.fullName = fullName;
    this.email = email;
    this.role = role;
    this.emailVerified = emailVerified;
  }

  public static UserResponse from(User user) {
    return new UserResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        user.getRole().getRoleName(),
        user.isEmailVerified());
  }

  public Long getId() {
    return id;
  }

  public String getFullName() {
    return fullName;
  }

  public String getEmail() {
    return email;
  }

  public String getRole() {
    return role;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }
}

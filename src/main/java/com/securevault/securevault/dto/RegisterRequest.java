package com.securevault.securevault.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

  @NotBlank(message = "{user.validation.full-name-required}")
  @Size(max = 100, message = "{user.validation.full-name-too-long}")
  private String fullName;

  @NotBlank(message = "{user.validation.email-required}")
  @Email(message = "{user.validation.email-invalid}")
  private String email;

  @NotBlank(message = "{user.validation.password-required}")
  @Size(min = 8, max = 72, message = "{user.validation.password-length}")
  private String password;

  public RegisterRequest() {}

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}

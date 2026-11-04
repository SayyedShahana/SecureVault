package com.securevault.securevault.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

  @NotBlank(message = "{auth.validation.email-required}")
  @Email(message = "{auth.validation.email-invalid}")
  private String email;

  @NotBlank(message = "{auth.validation.password-required}")
  @Size(max = 72, message = "{auth.validation.password-too-long}")
  private String password;

  public LoginRequest() {}

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

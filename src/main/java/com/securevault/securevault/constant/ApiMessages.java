package com.securevault.securevault.constant;

public final class ApiMessages {

  private ApiMessages() {
    // Prevent object creation
  }

  public static final String INVALID_REQUEST = "Invalid request data";

  public static final String UNEXPECTED_ERROR = "An unexpected error occurred";

  public static final class User {

    private User() {
      // Prevent object creation
    }

    public static final String REGISTRATION_SUCCESS = "Registration successful";

    public static final String EMAIL_ALREADY_REGISTERED = "Email is already registered";
  }
}

package com.securevault.securevault.common;

public final class MessageKeys {

  public static final String AUTH_REGISTER_SUCCESS = "auth.register.success";
  public static final String AUTH_LOGIN_SUCCESS = "auth.login.success";
  public static final String AUTH_LOGIN_LOCKED = "auth.login.locked";
  public static final String AUTH_LOGIN_INVALID_CREDENTIALS = "auth.login.invalid-credentials";
  public static final String AUTH_REFRESH_SUCCESS = "auth.refresh.success";
  public static final String AUTH_LOGOUT_SUCCESS = "auth.logout.success";
  public static final String AUTH_LOGOUT_SESSION_REVOKED = "auth.logout.session-revoked";
  public static final String AUTH_TOKEN_REQUIRED = "auth.token.required";
  public static final String AUTH_TOKEN_INVALID = "auth.token.invalid";
  public static final String AUTH_TOKEN_EXPIRED = "auth.token.expired";

  public static final String USER_REGISTER_EMAIL_EXISTS = "user.register.email-exists";
  public static final String USER_ROLE_DEFAULT_MISSING = "user.role.default-missing";
  public static final String USER_FOUND = "user.found";
  public static final String USER_NOT_FOUND = "user.not-found";

  public static final String ERROR_UNEXPECTED = "error.unexpected";
  public static final String ERROR_INVALID_REQUEST = "error.invalid-request";
  public static final String CRYPTO_SHA256_UNAVAILABLE = "crypto.sha256.unavailable";

  private MessageKeys() {}
}

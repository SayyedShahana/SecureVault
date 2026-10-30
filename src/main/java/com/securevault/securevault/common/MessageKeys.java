package com.securevault.securevault.common;

public final class MessageKeys {

  // Authentication messages
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

  // Authentication validation
  public static final String AUTH_VALIDATION_EMAIL_REQUIRED = "auth.validation.email-required";
  public static final String AUTH_VALIDATION_EMAIL_INVALID = "auth.validation.email-invalid";
  public static final String AUTH_VALIDATION_PASSWORD_REQUIRED =
      "auth.validation.password-required";
  public static final String AUTH_VALIDATION_PASSWORD_TOO_LONG =
      "auth.validation.password-too-long";

  // User messages
  public static final String USER_REGISTER_EMAIL_EXISTS = "user.register.email-exists";
  public static final String USER_ROLE_DEFAULT_MISSING = "user.role.default-missing";
  public static final String USER_FOUND = "user.found";
  public static final String USER_NOT_FOUND = "user.not-found";
  public static final String USER_AUTHENTICATED_NOT_FOUND = "user.authenticated.not-found";

  // User validation
  public static final String USER_VALIDATION_FULL_NAME_REQUIRED =
      "user.validation.full-name-required";
  public static final String USER_VALIDATION_FULL_NAME_TOO_LONG =
      "user.validation.full-name-too-long";
  public static final String USER_VALIDATION_EMAIL_REQUIRED = "user.validation.email-required";
  public static final String USER_VALIDATION_EMAIL_INVALID = "user.validation.email-invalid";
  public static final String USER_VALIDATION_PASSWORD_REQUIRED =
      "user.validation.password-required";
  public static final String USER_VALIDATION_PASSWORD_LENGTH = "user.validation.password-length";
  // Admin messages
  public static final String ADMIN_CONFIGURATION_REQUIRED = "admin.configuration.required";

  public static final String ADMIN_EMAIL_ALREADY_REGISTERED = "admin.email.already-registered";

  public static final String ADMIN_ROLE_NOT_FOUND = "admin.role.not-found";
  public static final String ADMIN_USERS_SUCCESS = "admin.users.success";
  public static final String ADMIN_AUDIT_LOGS_SUCCESS = "admin.audit-logs.success";
  // Document success messages
  public static final String DOCUMENT_UPLOAD_SUCCESS = "document.upload.success";
  public static final String DOCUMENT_LIST_SUCCESS = "document.list.success";
  public static final String DOCUMENT_RENAME_SUCCESS = "document.rename.success";
  public static final String DOCUMENT_DELETE_SUCCESS = "document.delete.success";

  // Document errors
  public static final String DOCUMENT_NOT_FOUND = "document.not-found";
  public static final String DOCUMENT_STORAGE_UNAVAILABLE = "document.storage.unavailable";
  public static final String DOCUMENT_PREVIEW_UNSUPPORTED = "document.preview.unsupported";

  // Document sorting
  public static final String DOCUMENT_SORT_INVALID_FIELD = "document.sort.invalid-field";
  public static final String DOCUMENT_SORT_INVALID_DIRECTION = "document.sort.invalid-direction";

  // Document rename validation
  public static final String DOCUMENT_RENAME_FILENAME_REQUIRED =
      "document.rename.filename-required";
  public static final String DOCUMENT_RENAME_FILENAME_EMPTY = "document.rename.filename-empty";
  public static final String DOCUMENT_RENAME_FILENAME_TOO_LONG =
      "document.rename.filename-too-long";
  public static final String DOCUMENT_RENAME_INVALID_FILENAME = "document.rename.invalid-filename";
  public static final String DOCUMENT_RENAME_EXTENSION_CHANGE = "document.rename.extension-change";

  // Document validation
  public static final String DOCUMENT_VALIDATION_EMPTY = "document.validation.empty";
  public static final String DOCUMENT_VALIDATION_SIZE_EXCEEDED =
      "document.validation.size-exceeded";
  public static final String DOCUMENT_VALIDATION_INVALID_FILENAME =
      "document.validation.invalid-filename";
  public static final String DOCUMENT_VALIDATION_UNSUPPORTED_EXTENSION =
      "document.validation.unsupported-extension";
  public static final String DOCUMENT_VALIDATION_TYPE_NOT_ALLOWED =
      "document.validation.type-not-allowed";
  public static final String DOCUMENT_VALIDATION_MIME_MISMATCH =
      "document.validation.mime-mismatch";
  public static final String DOCUMENT_VALIDATION_CONTENT_MISMATCH =
      "document.validation.content-mismatch";

  // File storage
  public static final String STORAGE_DESTINATION_INVALID = "storage.destination.invalid";
  public static final String STORAGE_KEY_REQUIRED = "storage.key.required";
  public static final String STORAGE_KEY_INVALID = "storage.key.invalid";

  // General errors
  public static final String ERROR_UNEXPECTED = "error.unexpected";
  public static final String ERROR_INVALID_REQUEST = "error.invalid-request";
  public static final String ERROR_UPLOAD_SIZE_EXCEEDED = "error.upload-size-exceeded";

  // Cryptographic messages
  public static final String CRYPTO_SHA256_UNAVAILABLE = "crypto.sha256.unavailable";

  // Refresh token messages
  public static final String AUTH_REFRESH_TOKEN_REQUIRED = "auth.refresh.token-required";

  public static final String AUTH_REFRESH_TOKEN_INVALID = "auth.refresh.token-invalid";

  public static final String AUTH_REFRESH_TOKEN_EXPIRED = "auth.refresh.token-expired";

  private MessageKeys() {}
}

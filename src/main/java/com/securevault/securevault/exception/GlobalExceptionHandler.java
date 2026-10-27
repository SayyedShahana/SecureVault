package com.securevault.securevault.exception;

import com.securevault.securevault.common.MessageKeys;
import com.securevault.securevault.dto.ApiResponse;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private final MessageSource messageSource;
  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  public GlobalExceptionHandler(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException exception) {

    String exceptionMessage = exception.getMessage();
    String message;

    if ("Invalid email or password".equals(exceptionMessage)) {
      message = exceptionMessage;
    } else if (exceptionMessage != null
        && exceptionMessage.startsWith("Too many failed login attempts")) {
      message = messageSource.getMessage(MessageKeys.AUTH_LOGIN_LOCKED, null, Locale.getDefault());
    } else {
      message = messageSource.getMessage(MessageKeys.ERROR_INVALID_REQUEST, null, Locale.getDefault());
    }

    ApiResponse<Void> response = new ApiResponse<>(false, message, null);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationErrors(
      MethodArgumentNotValidException exception) {

    String message = exception.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(error -> error.getDefaultMessage())
        .filter(value -> value != null && !value.isBlank())
        .orElseGet(
            () -> messageSource.getMessage(
                MessageKeys.ERROR_INVALID_REQUEST, null, Locale.getDefault()));

    ApiResponse<Void> response = new ApiResponse<>(false, message, null);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception exception) {

    String message = messageSource.getMessage(MessageKeys.ERROR_UNEXPECTED, null, Locale.getDefault());

    ApiResponse<Void> response = new ApiResponse<>(false, message, null);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}

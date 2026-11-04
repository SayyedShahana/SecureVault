package com.securevault.securevault.exception;

import com.securevault.securevault.common.MessageKeys;
import com.securevault.securevault.dto.ApiResponse;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private final MessageSource messageSource;

  public GlobalExceptionHandler(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException exception) {

    String exceptionMessage = exception.getMessage();
    String message;

    if (exceptionMessage == null || exceptionMessage.isBlank()) {
      message = getMessage(MessageKeys.ERROR_INVALID_REQUEST);
    } else if (exceptionMessage.equals(getMessage(MessageKeys.AUTH_LOGIN_INVALID_CREDENTIALS))) {
      message = getMessage(MessageKeys.AUTH_LOGIN_INVALID_CREDENTIALS);
    } else if (exceptionMessage.startsWith(getMessage(MessageKeys.AUTH_LOGIN_LOCKED))) {
      message = getMessage(MessageKeys.AUTH_LOGIN_LOCKED);
    } else {
      message = exceptionMessage;
    }

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiResponse<>(false, message, null));
  }

  @ExceptionHandler(DocumentNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleDocumentNotFound(
      DocumentNotFoundException exception) {

    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiResponse<>(false, exception.getMessage(), null));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationErrors(
      MethodArgumentNotValidException exception) {

    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getDefaultMessage())
            .filter(value -> value != null && !value.isBlank())
            .orElseGet(() -> getMessage(MessageKeys.ERROR_INVALID_REQUEST));

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiResponse<>(false, message, null));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(
      MaxUploadSizeExceededException exception) {

    String message = getMessage(MessageKeys.ERROR_UPLOAD_SIZE_EXCEEDED);

    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(new ApiResponse<>(false, message, null));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception exception) {

    logger.error("An unexpected error occurred while processing the request.", exception);

    String message = getMessage(MessageKeys.ERROR_UNEXPECTED);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiResponse<>(false, message, null));
  }

  private String getMessage(String messageKey) {
    return messageSource.getMessage(messageKey, null, Locale.getDefault());
  }
}

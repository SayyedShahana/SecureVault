package com.securevault.securevault.service;

import com.securevault.securevault.common.MessageKeys;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentValidationService {

  private final long maxFileSizeBytes;
  private final MessageSource messageSource;

  private static final Map<String, String> ALLOWED_EXTENSIONS =
      Map.of(
          "pdf", "application/pdf",
          "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          "txt", "text/plain",
          "png", "image/png",
          "jpg", "image/jpeg",
          "jpeg", "image/jpeg");

  public DocumentValidationService(
      @Value("${securevault.storage.max-file-size-bytes}") long maxFileSizeBytes,
      MessageSource messageSource) {
    this.maxFileSizeBytes = maxFileSizeBytes;
    this.messageSource = messageSource;
  }

  public void validate(MultipartFile file) throws IOException {

    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException(getMessage(MessageKeys.DOCUMENT_VALIDATION_EMPTY));
    }

    if (file.getSize() > maxFileSizeBytes) {
      throw new IllegalArgumentException(getMessage(MessageKeys.DOCUMENT_VALIDATION_SIZE_EXCEEDED));
    }

    String originalFilename = file.getOriginalFilename();

    if (originalFilename == null
        || originalFilename.isBlank()
        || originalFilename.contains("/")
        || originalFilename.contains("\\")
        || originalFilename.contains("..")) {
      throw new IllegalArgumentException(
          getMessage(MessageKeys.DOCUMENT_VALIDATION_INVALID_FILENAME));
    }

    int dotIndex = originalFilename.lastIndexOf('.');

    if (dotIndex <= 0 || dotIndex == originalFilename.length() - 1) {
      throw new IllegalArgumentException(
          getMessage(MessageKeys.DOCUMENT_VALIDATION_UNSUPPORTED_EXTENSION));
    }

    String extension = originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);

    String expectedMimeType = ALLOWED_EXTENSIONS.get(extension);

    if (expectedMimeType == null) {
      throw new IllegalArgumentException(
          getMessage(MessageKeys.DOCUMENT_VALIDATION_TYPE_NOT_ALLOWED));
    }

    String suppliedMimeType = file.getContentType();

    if (suppliedMimeType == null || !expectedMimeType.equalsIgnoreCase(suppliedMimeType)) {
      throw new IllegalArgumentException(getMessage(MessageKeys.DOCUMENT_VALIDATION_MIME_MISMATCH));
    }

    byte[] content = file.getBytes();

    if (!hasValidContent(extension, content)) {
      throw new IllegalArgumentException(
          getMessage(MessageKeys.DOCUMENT_VALIDATION_CONTENT_MISMATCH));
    }
  }

  public boolean isPreviewSupported(String mimeType) {
    return mimeType != null
        && (mimeType.equalsIgnoreCase("application/pdf")
            || mimeType.equalsIgnoreCase("image/png")
            || mimeType.equalsIgnoreCase("image/jpeg"));
  }

  private boolean hasValidContent(String extension, byte[] bytes) {

    switch (extension) {
      case "pdf":
        return startsWith(bytes, new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D});

      case "png":
        return startsWith(
            bytes, new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

      case "jpg":
      case "jpeg":
        return bytes.length >= 3
            && bytes[0] == (byte) 0xFF
            && bytes[1] == (byte) 0xD8
            && bytes[2] == (byte) 0xFF;

      case "docx":
        return isValidDocx(bytes);

      case "xlsx":
        return isValidXlsx(bytes);

      case "txt":
        return isValidText(bytes);

      default:
        return false;
    }
  }

  private boolean isValidDocx(byte[] bytes) {
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
      return true;
    } catch (IOException | RuntimeException ex) {
      return false;
    }
  }

  private boolean isValidXlsx(byte[] bytes) {
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      return true;
    } catch (IOException | RuntimeException ex) {
      return false;
    }
  }

  private boolean isValidText(byte[] bytes) {
    try {
      String content =
          StandardCharsets.UTF_8
              .newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT)
              .decode(ByteBuffer.wrap(bytes))
              .toString();

      for (int i = 0; i < content.length(); i++) {
        char character = content.charAt(i);

        if (Character.isISOControl(character)
            && character != '\n'
            && character != '\r'
            && character != '\t'
            && character != '\f') {
          return false;
        }
      }

      return true;

    } catch (CharacterCodingException ex) {
      return false;
    }
  }

  private boolean startsWith(byte[] bytes, byte[] signature) {

    if (bytes.length < signature.length) {
      return false;
    }

    for (int i = 0; i < signature.length; i++) {
      if (bytes[i] != signature[i]) {
        return false;
      }
    }

    return true;
  }

  private String getMessage(String messageKey) {
    return messageSource.getMessage(messageKey, null, Locale.getDefault());
  }
}

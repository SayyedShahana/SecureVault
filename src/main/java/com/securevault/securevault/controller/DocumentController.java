package com.securevault.securevault.controller;

import com.securevault.securevault.common.MessageKeys;
import com.securevault.securevault.dto.ApiResponse;
import com.securevault.securevault.dto.DocumentResponse;
import com.securevault.securevault.dto.RenameDocumentRequest;
import com.securevault.securevault.entity.Document;
import com.securevault.securevault.service.DocumentService;
import com.securevault.securevault.service.FileStorageService;
import jakarta.validation.Valid;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.MessageSource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

  private final DocumentService documentService;
  private final FileStorageService fileStorageService;
  private final MessageSource messageSource;

  public DocumentController(
      DocumentService documentService,
      FileStorageService fileStorageService,
      MessageSource messageSource) {
    this.documentService = documentService;
    this.fileStorageService = fileStorageService;
    this.messageSource = messageSource;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<DocumentUploadResponse>> uploadDocument(
      @RequestPart("file") MultipartFile file, Authentication authentication) throws IOException {

    Document document = documentService.uploadDocument(authentication.getName(), file);

    DocumentUploadResponse response =
        new DocumentUploadResponse(
            document.getId(),
            document.getFileName(),
            document.getMimeType(),
            document.getFileSizeBytes(),
            document.getStatus(),
            document.getCreatedAt());

    ApiResponse<DocumentUploadResponse> apiResponse =
        new ApiResponse<>(
            true,
            messageSource.getMessage(
                MessageKeys.DOCUMENT_UPLOAD_SUCCESS, null, Locale.getDefault()),
            response);

    return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocuments(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String mimeType,
      @RequestParam(required = false) String sortBy,
      @RequestParam(required = false) String direction,
      Authentication authentication) {

    List<DocumentResponse> documents =
        documentService
            .listDocuments(authentication.getName(), search, mimeType, sortBy, direction)
            .stream()
            .map(this::toDocumentResponse)
            .toList();

    ApiResponse<List<DocumentResponse>> apiResponse =
        new ApiResponse<>(
            true,
            messageSource.getMessage(MessageKeys.DOCUMENT_LIST_SUCCESS, null, Locale.getDefault()),
            documents);

    return ResponseEntity.ok(apiResponse);
  }

  @GetMapping("/{documentId}/download")
  public ResponseEntity<Resource> downloadDocument(
      @PathVariable UUID documentId, Authentication authentication) throws IOException {

    Document document =
        documentService.getDocumentForDownload(authentication.getName(), documentId);

    Path filePath = fileStorageService.getFilePath(document.getStorageKey());

    if (!Files.isRegularFile(filePath)) {
      throw new FileNotFoundException(
          messageSource.getMessage(
              MessageKeys.DOCUMENT_STORAGE_UNAVAILABLE, null, Locale.getDefault()));
    }

    Resource resource = new InputStreamResource(Files.newInputStream(filePath));

    ContentDisposition contentDisposition =
        ContentDisposition.attachment().filename(document.getFileName()).build();

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(document.getMimeType()))
        .contentLength(document.getFileSizeBytes())
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
        .header("X-Content-Type-Options", "nosniff")
        .body(resource);
  }

  @GetMapping("/{documentId}/preview")
  public ResponseEntity<Resource> previewDocument(
      @PathVariable UUID documentId, Authentication authentication) throws IOException {

    Document document = documentService.getDocumentForPreview(authentication.getName(), documentId);

    Path filePath = fileStorageService.getFilePath(document.getStorageKey());

    if (!Files.isRegularFile(filePath)) {
      throw new FileNotFoundException(
          messageSource.getMessage(
              MessageKeys.DOCUMENT_STORAGE_UNAVAILABLE, null, Locale.getDefault()));
    }

    Resource resource = new InputStreamResource(Files.newInputStream(filePath));

    ContentDisposition contentDisposition =
        ContentDisposition.inline().filename(document.getFileName()).build();

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(document.getMimeType()))
        .contentLength(document.getFileSizeBytes())
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
        .header("X-Content-Type-Options", "nosniff")
        .body(resource);
  }

  @PatchMapping("/{documentId}/rename")
  public ResponseEntity<ApiResponse<DocumentResponse>> renameDocument(
      @PathVariable UUID documentId,
      @Valid @RequestBody RenameDocumentRequest request,
      Authentication authentication) {

    Document document =
        documentService.renameDocument(authentication.getName(), documentId, request.newFileName());

    DocumentResponse response = toDocumentResponse(document);

    ApiResponse<DocumentResponse> apiResponse =
        new ApiResponse<>(
            true,
            messageSource.getMessage(
                MessageKeys.DOCUMENT_RENAME_SUCCESS, null, Locale.getDefault()),
            response);

    return ResponseEntity.ok(apiResponse);
  }

  @DeleteMapping("/{documentId}")
  public ResponseEntity<ApiResponse<Void>> deleteDocument(
      @PathVariable UUID documentId, Authentication authentication) throws IOException {

    documentService.deleteDocument(authentication.getName(), documentId);

    ApiResponse<Void> apiResponse =
        new ApiResponse<>(
            true,
            messageSource.getMessage(
                MessageKeys.DOCUMENT_DELETE_SUCCESS, null, Locale.getDefault()),
            null);

    return ResponseEntity.ok(apiResponse);
  }

  private DocumentResponse toDocumentResponse(Document document) {
    return new DocumentResponse(
        document.getId(),
        document.getFileName(),
        document.getMimeType(),
        document.getFileSizeBytes(),
        document.getStatus(),
        document.getCreatedAt(),
        document.getUpdatedAt());
  }

  public record DocumentUploadResponse(
      UUID documentId,
      String fileName,
      String mimeType,
      long fileSizeBytes,
      String status,
      java.time.OffsetDateTime createdAt) {}
}


package com.securevault.securevault.controller;

import com.securevault.securevault.dto.ApiResponse;
import com.securevault.securevault.entity.Document;
import com.securevault.securevault.service.DocumentService;
import com.securevault.securevault.service.FileStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final FileStorageService fileStorageService;

    public DocumentController(
            DocumentService documentService, FileStorageService fileStorageService) {
        this.documentService = documentService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentUploadResponse>> uploadDocument(
            @RequestPart("file") MultipartFile file, Authentication authentication)
            throws IOException {

        Document document = documentService.uploadDocument(authentication.getName(), file);

        DocumentUploadResponse response = new DocumentUploadResponse(
                document.getId(),
                document.getFileName(),
                document.getMimeType(),
                document.getFileSizeBytes(),
                document.getStatus(),
                document.getCreatedAt());

        ApiResponse<DocumentUploadResponse> apiResponse = new ApiResponse<>(true, "Document uploaded successfully.",
                response);

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable UUID documentId, Authentication authentication) throws IOException {

        Document document = documentService.getDocumentForDownload(authentication.getName(), documentId);

        Path filePath = fileStorageService.getFilePath(document.getStorageKey());

        if (!Files.isRegularFile(filePath)) {
            throw new java.io.FileNotFoundException("Stored document is unavailable.");
        }

        Resource resource = new InputStreamResource(Files.newInputStream(filePath));

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(document.getFileName())
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getMimeType()))
                .contentLength(document.getFileSizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    public record DocumentUploadResponse(
            UUID documentId,
            String fileName,
            String mimeType,
            long fileSizeBytes,
            String status,
            java.time.OffsetDateTime createdAt) {
    }
}

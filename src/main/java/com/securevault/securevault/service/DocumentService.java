
package com.securevault.securevault.service;

import com.securevault.securevault.entity.Document;
import com.securevault.securevault.entity.User;
import com.securevault.securevault.repository.DocumentRepository;
import com.securevault.securevault.repository.UserRepository;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.securevault.securevault.exception.DocumentNotFoundException;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentValidationService validationService;
    private final FileStorageService fileStorageService;

    public DocumentService(
            DocumentRepository documentRepository,
            UserRepository userRepository,
            DocumentValidationService validationService,
            FileStorageService fileStorageService) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.validationService = validationService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public Document uploadDocument(String email, MultipartFile file) throws IOException {
        validationService.validate(file);

        User owner = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user was not found."));

        UUID storageKey = fileStorageService.storeFile(file);

        try {
            Document document = new Document();
            document.setOwner(owner);
            document.setFileName(file.getOriginalFilename());
            document.setStorageKey(storageKey);
            document.setMimeType(file.getContentType());
            document.setFileSizeBytes(file.getSize());
            document.setFileHash(calculateSha256(file));
            document.setStatus("ACTIVE");

            return documentRepository.save(document);
        } catch (RuntimeException ex) {
            try {
                fileStorageService.deleteFile(storageKey);
            } catch (IOException cleanupException) {
                ex.addSuppressed(cleanupException);
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Document getDocumentForDownload(String email, UUID documentId) {
        User owner = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Authenticated user was not found."));

        return documentRepository
                .findByIdAndOwner_IdAndStatus(documentId, owner.getId(), "ACTIVE")
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found."));
    }

    private String calculateSha256(MultipartFile file) throws IOException {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(file.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }
}

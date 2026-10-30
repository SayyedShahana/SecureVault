package com.securevault.securevault.repository;

import com.securevault.securevault.entity.Document;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
  List<Document> findByOwner_IdAndStatus(Long ownerId, String status);

  List<Document> findByOwner_IdAndStatus(Long ownerId, String status, Sort sort);

  List<Document> findByOwner_IdAndStatusAndFileNameContainingIgnoreCase(
      Long ownerId, String status, String fileName, Sort sort);

  List<Document> findByOwner_IdAndStatusAndMimeType(
      Long ownerId, String status, String mimeType, Sort sort);

  List<Document> findByOwner_IdAndStatusAndFileNameContainingIgnoreCaseAndMimeType(
      Long ownerId, String status, String fileName, String mimeType, Sort sort);

  Optional<Document> findByIdAndOwner_IdAndStatus(UUID documentId, Long ownerId, String status);
}

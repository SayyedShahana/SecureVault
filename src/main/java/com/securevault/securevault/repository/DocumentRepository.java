
package com.securevault.securevault.repository;

import com.securevault.securevault.entity.Document;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByOwner_IdAndStatus(
            Long ownerId, String status);

    Optional<Document> findByIdAndOwner_IdAndStatus(
            UUID documentId, Long ownerId, String status);
}

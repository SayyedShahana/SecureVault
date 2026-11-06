package com.securevault.securevault.repository;

import com.securevault.securevault.entity.DocumentShare;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentShareRepository extends JpaRepository<DocumentShare, UUID> {

    @Query("""
            SELECT s FROM DocumentShare s
            WHERE s.sharedWith.id = :userId
            AND s.canView = true
            AND s.revokedAt IS NULL
            AND (s.expiresAt IS NULL OR s.expiresAt > :now)
            """)
    List<DocumentShare> findActiveSharesForUser(
            @Param("userId") Long userId, @Param("now") OffsetDateTime now);

    @Query("""
            SELECT s FROM DocumentShare s
            WHERE s.document.id = :documentId
            AND s.revokedAt IS NULL
            AND (s.expiresAt IS NULL OR s.expiresAt > :now)
            """)
    List<DocumentShare> findActiveSharesForDocument(
            @Param("documentId") UUID documentId, @Param("now") OffsetDateTime now);

    @Query("""
            SELECT s FROM DocumentShare s
            WHERE s.document.id = :documentId
            AND s.sharedWith.id = :userId
            AND s.revokedAt IS NULL
            AND (s.expiresAt IS NULL OR s.expiresAt > :now)
            """)
    Optional<DocumentShare> findActiveShare(
            @Param("documentId") UUID documentId,
            @Param("userId") Long userId,
            @Param("now") OffsetDateTime now);

    @Query("""
            SELECT s FROM DocumentShare s
            WHERE s.id = :shareId
            AND s.document.id = :documentId
            AND s.document.owner.id = :ownerId
            AND s.revokedAt IS NULL
            """)
    Optional<DocumentShare> findShareOwnedBy(
            @Param("shareId") UUID shareId,
            @Param("documentId") UUID documentId,
            @Param("ownerId") Long ownerId);

    @Query("""
            SELECT s FROM DocumentShare s
            JOIN FETCH s.document
            JOIN FETCH s.sharedWith
            WHERE s.sharedBy.id = :userId
            AND s.revokedAt IS NULL
            AND (s.expiresAt IS NULL OR s.expiresAt > :now)
            ORDER BY s.createdAt DESC
            """)
    List<DocumentShare> findActiveSharesByOwner(
            @Param("userId") Long userId,
            @Param("now") OffsetDateTime now);
}

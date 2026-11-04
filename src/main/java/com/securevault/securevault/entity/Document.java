package com.securevault.securevault.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "document_id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(name = "storage_key", nullable = false, unique = true, updatable = false)
  private UUID storageKey;

  @Column(name = "mime_type", nullable = false)
  private String mimeType;

  @Column(name = "file_size_bytes", nullable = false)
  private long fileSizeBytes;

  @Column(name = "file_hash", nullable = false)
  private String fileHash;

  @Column(name = "encryption_algorithm")
  private String encryptionAlgorithm;

  @Column(name = "encryption_key_id")
  private String encryptionKeyId;

  @Column(name = "iv_or_nonce")
  private byte[] ivOrNonce;

  @Column(name = "auth_tag")
  private byte[] authTag;

  @Column(name = "status", nullable = false)
  private String status = "ACTIVE";

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  public Document() {}

  @PrePersist
  protected void onCreate() {
    OffsetDateTime now = OffsetDateTime.now();

    if (createdAt == null) {
      createdAt = now;
    }

    if (updatedAt == null) {
      updatedAt = now;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }

  public UUID getId() {
    return id;
  }

  public User getOwner() {
    return owner;
  }

  public void setOwner(User owner) {
    this.owner = owner;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public UUID getStorageKey() {
    return storageKey;
  }

  public void setStorageKey(UUID storageKey) {
    this.storageKey = storageKey;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public long getFileSizeBytes() {
    return fileSizeBytes;
  }

  public void setFileSizeBytes(long fileSizeBytes) {
    this.fileSizeBytes = fileSizeBytes;
  }

  public String getFileHash() {
    return fileHash;
  }

  public void setFileHash(String fileHash) {
    this.fileHash = fileHash;
  }

  public String getEncryptionAlgorithm() {
    return encryptionAlgorithm;
  }

  public void setEncryptionAlgorithm(String encryptionAlgorithm) {
    this.encryptionAlgorithm = encryptionAlgorithm;
  }

  public String getEncryptionKeyId() {
    return encryptionKeyId;
  }

  public void setEncryptionKeyId(String encryptionKeyId) {
    this.encryptionKeyId = encryptionKeyId;
  }

  public byte[] getIvOrNonce() {
    return ivOrNonce;
  }

  public void setIvOrNonce(byte[] ivOrNonce) {
    this.ivOrNonce = ivOrNonce;
  }

  public byte[] getAuthTag() {
    return authTag;
  }

  public void setAuthTag(byte[] authTag) {
    this.authTag = authTag;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}

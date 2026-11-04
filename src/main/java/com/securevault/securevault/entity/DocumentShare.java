package com.securevault.securevault.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_shares")
public class DocumentShare {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "share_id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "document_id", nullable = false)
  private Document document;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "shared_by", nullable = false)
  private User sharedBy;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "shared_with", nullable = false)
  private User sharedWith;

  @Column(name = "can_view", nullable = false)
  private boolean canView = true;

  @Column(name = "can_download", nullable = false)
  private boolean canDownload = false;

  @Column(name = "can_share", nullable = false)
  private boolean canShare = false;

  @Column(name = "expires_at")
  private OffsetDateTime expiresAt;

  @Column(name = "revoked_at")
  private OffsetDateTime revokedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  public DocumentShare() {}

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = OffsetDateTime.now();
    }
  }

  public UUID getId() {
    return id;
  }

  public Document getDocument() {
    return document;
  }

  public void setDocument(Document document) {
    this.document = document;
  }

  public User getSharedBy() {
    return sharedBy;
  }

  public void setSharedBy(User sharedBy) {
    this.sharedBy = sharedBy;
  }

  public User getSharedWith() {
    return sharedWith;
  }

  public void setSharedWith(User sharedWith) {
    this.sharedWith = sharedWith;
  }

  public boolean isCanView() {
    return canView;
  }

  public void setCanView(boolean canView) {
    this.canView = canView;
  }

  public boolean isCanDownload() {
    return canDownload;
  }

  public void setCanDownload(boolean canDownload) {
    this.canDownload = canDownload;
  }

  public boolean isCanShare() {
    return canShare;
  }

  public void setCanShare(boolean canShare) {
    this.canShare = canShare;
  }

  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(OffsetDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  public OffsetDateTime getRevokedAt() {
    return revokedAt;
  }

  public void setRevokedAt(OffsetDateTime revokedAt) {
    this.revokedAt = revokedAt;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}

package com.securevault.securevault.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;

public class ShareDocumentRequest {

  @NotBlank(message = "{document.share.email.required}")
  @Email(message = "{document.share.email.invalid}")
  private String email;

  private boolean canView = true;

  private boolean canDownload = false;

  private boolean canShare = false;

  private OffsetDateTime expiresAt;

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
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
}

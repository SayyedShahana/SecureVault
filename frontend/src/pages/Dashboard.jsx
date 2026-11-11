
import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { documentApi, auditLogApi } from "../services/api";
import "./Dashboard.css";
import "./Documents.css";
import "./AuditLogs.css";

function formatSize(bytes = 0) {
  if (bytes === 0) return "0 Bytes";

  const units = ["Bytes", "KB", "MB", "GB"];
  const index = Math.floor(Math.log(bytes) / Math.log(1024));

  return `${(bytes / Math.pow(1024, index)).toFixed(2)} ${units[index]}`;
}

function formatDateTime(date) {
  if (!date) return "N/A";

  return new Date(date).toLocaleString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: true,
  });
}

function formatFileType(mimeType) {
  if (!mimeType) return "FILE";

  return mimeType.split("/").pop().toUpperCase();
}

function PermissionBadge({ allowed }) {
  return (
    <span
      className={`permission-badge ${
        allowed ? "permission-allowed" : "permission-denied"
      }`}
    >
      {allowed ? "Allowed" : "Not allowed"}
    </span>
  );
}

function Dashboard() {
  const { user, role, logout } = useAuth();
  const navigate = useNavigate();

  const [documents, setDocuments] = useState([]);
  const [sharedDocuments, setSharedDocuments] = useState([]);
  const [totalDocuments, setTotalDocuments] = useState(0);
  const [storageUsed, setStorageUsed] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);

  const [recentAuditLogs, setRecentAuditLogs] = useState([]);
  const [auditLogsLoading, setAuditLogsLoading] = useState(true);

  // Share modal states
  const [shareDocument, setShareDocument] = useState(null);
  const [shareEmail, setShareEmail] = useState("");
  const [canView, setCanView] = useState(true);
  const [canDownload, setCanDownload] = useState(false);
  const [canShare, setCanShare] = useState(false);
  const [expiresAt, setExpiresAt] = useState("");
  const [sharing, setSharing] = useState(false);

  // Load dashboard documents and shared documents
  useEffect(() => {
    const loadDashboard = async () => {
      try {
        setLoading(true);
        setError("");

        const response = await documentApi.getDocuments({
          page: 0,
          size: 5,
          sortBy: "createdAt",
          direction: "desc",
        });

        const pageData = response.data;

        setDocuments(pageData.content || []);
        setTotalDocuments(pageData.totalElements || 0);

        // Calculate storage used across all document pages.
        let totalSize = 0;

        for (
          let currentPage = 0;
          currentPage < pageData.totalPages;
          currentPage++
        ) {
          const pageResponse =
            currentPage === 0
              ? pageData
              : (
                  await documentApi.getDocuments({
                    page: currentPage,
                    size: 5,
                    sortBy: "createdAt",
                    direction: "desc",
                  })
                ).data;

          totalSize += (pageResponse.content || []).reduce(
            (total, document) => total + (document.fileSizeBytes || 0),
            0
          );
        }

        setStorageUsed(totalSize);

        const sharedResponse = await documentApi.getSharedDocuments();
        setSharedDocuments(sharedResponse.data || []);
      } catch (err) {
        setError(err.message || "Unable to load dashboard.");
      } finally {
        setLoading(false);
      }
    };

    loadDashboard();
  }, []);

  // Load the latest five audit log records
  useEffect(() => {
    const loadRecentActivity = async () => {
      try {
        setAuditLogsLoading(true);

        const response = await auditLogApi.getMyAuditLogs();

        setRecentAuditLogs((response.data || []).slice(0, 5));
      } catch (err) {
        console.error("Unable to load recent activity:", err);
      } finally {
        setAuditLogsLoading(false);
      }
    };

    loadRecentActivity();
  }, []);

  const handleLogout = async () => {
  setIsProfileMenuOpen(false);

  try {
    await logout();
  } finally {
    navigate("/login", { replace: true });
  }
};

  // Open share modal
  const openShareModal = (share) => {
    setActionError("");

    setShareDocument({
      documentId: share.documentId,
      fileName: share.documentName,
    });

    setShareEmail("");
    setCanView(true);
    setCanDownload(false);
    setCanShare(false);
    setExpiresAt("");
  };

  // Close share modal
  const closeShareModal = () => {
    if (sharing) return;

    setShareDocument(null);
    setShareEmail("");
    setCanView(true);
    setCanDownload(false);
    setCanShare(false);
    setExpiresAt("");
  };

  // Share document
  const handleShare = async (event) => {
    event.preventDefault();
    setActionError("");

    if (!shareDocument || !shareEmail.trim()) {
      setActionError("Please enter the recipient's email.");
      return;
    }

    if (!canView && !canDownload && !canShare) {
      setActionError("Please select at least one permission.");
      return;
    }

    if (expiresAt && new Date(expiresAt).getTime() <= Date.now()) {
      setActionError("Expiry date must be in the future.");
      return;
    }

    try {
      setSharing(true);

      await documentApi.shareDocument(shareDocument.documentId, {
        email: shareEmail.trim(),
        canView,
        canDownload,
        canShare,
        expiresAt: expiresAt
          ? new Date(expiresAt).toISOString()
          : null,
      });

      setShareDocument(null);
      setShareEmail("");
      setCanView(true);
      setCanDownload(false);
      setCanShare(false);
      setExpiresAt("");

      window.alert("Document shared successfully.");
    } catch (err) {
      setActionError(err.message || "Unable to share document.");
    } finally {
      setSharing(false);
    }
  };

  // Preview shared document
  const handlePreview = async (share) => {
    setActionError("");

    const previewWindow = window.open("about:blank", "_blank");

    if (!previewWindow) {
      setActionError("Please allow pop-ups to preview documents.");
      return;
    }

    try {
      const response = await documentApi.previewDocument(share.documentId);
      const blob = await response.blob();
      const fileUrl = URL.createObjectURL(blob);

      previewWindow.location.href = fileUrl;

      setTimeout(() => URL.revokeObjectURL(fileUrl), 60000);
    } catch (err) {
      previewWindow.close();
      setActionError(err.message || "Unable to preview document.");
    }
  };

  // Download shared document
  const handleDownload = async (share) => {
    try {
      setActionError("");

      const response = await documentApi.downloadDocument(share.documentId);
      const blob = await response.blob();
      const fileUrl = URL.createObjectURL(blob);

      const link = document.createElement("a");
      link.href = fileUrl;
      link.download = share.documentName || "document";

      document.body.appendChild(link);
      link.click();
      link.remove();

      URL.revokeObjectURL(fileUrl);
    } catch (err) {
      setActionError(err.message || "Unable to download document.");
    }
  };

  const getOutcomeClass = (outcome) => {
    return outcome?.toUpperCase() === "SUCCESS"
      ? "audit-outcome-success"
      : "audit-outcome-failed";
  };

  return (
    <div className="dashboard-layout">
      <aside className="dashboard-sidebar">
        <Link to="/dashboard" className="dashboard-brand">
          <span className="dashboard-brand-icon">S</span>
          SecureVault
        </Link>

        
<nav className="dashboard-nav">
  <Link to="/dashboard" className="active">
    <span>▦</span> Dashboard
  </Link>

  <Link to="/documents">
    <span>▤</span> My Documents
  </Link>

  <Link to="/documents/upload">
    <span>↑</span> Upload Document
  </Link>

  <Link to="/shared-by-me">
    <span>↗</span> Shared By Me
  </Link>

  <Link to="/audit-logs">
    <span>▧</span> Activity History
  </Link>
</nav>
      </aside>

      <main className="dashboard-main">
        {/* Dashboard Header */}
        <header className="dashboard-header">
          <div>
            <h1>Dashboard</h1>
            <p>Manage your documents securely in one place.</p>
          </div>

          <div className="dashboard-header-actions">
           <div className="dashboard-profile-menu">
  <button
  type="button"
  className="dashboard-user-profile"
  onClick={() => {
    console.log("Profile button clicked");
    setIsProfileMenuOpen((previous) => {
      console.log("Menu state:", !previous);
      return !previous;
    });
  }}
  aria-expanded={isProfileMenuOpen}
>
    <div className="dashboard-user-avatar">
      {(user?.email || "U").charAt(0).toUpperCase()}
    </div>

    <div className="dashboard-user-info">
      <strong>{user?.email || "User"}</strong>
      <span>{role || user?.role || "USER"}</span>
    </div>

    <span className="profile-dropdown-arrow">
      {isProfileMenuOpen ? "⌃" : "⌄"}
    </span>
  </button>

  {isProfileMenuOpen && (
    <div className="dashboard-profile-dropdown">
      <div className="profile-dropdown-info">
        <strong>{user?.email || "User"}</strong>
        <span>{role || user?.role || "USER"} Account</span>
      </div>

      <button
        type="button"
        className="profile-logout-button"
        onClick={handleLogout}
      >
        ↪ Logout
      </button>
    </div>
  )}
</div>
            <Link
              to="/documents/upload"
              className="dashboard-upload-button"
            >
              + Upload Document
            </Link>
          </div>
        </header>

        {error && <p className="dashboard-error">{error}</p>}
        {actionError && <p className="dashboard-error">{actionError}</p>}

        {/* Dashboard Statistics */}
        <section className="dashboard-stats">
          <article className="dashboard-stat-card">
            <div className="stat-icon">▤</div>
            <p>Total Documents</p>
            <h2>{loading ? "..." : totalDocuments}</h2>
          </article>

          <article className="dashboard-stat-card">
            <div className="stat-icon">▣</div>
            <p>Storage Used</p>
            <h2>{loading ? "..." : formatSize(storageUsed)}</h2>
          </article>

          <article className="dashboard-stat-card">
            <div className="stat-icon">↗</div>
            <p>Recent Documents</p>
            <h2>{loading ? "..." : documents.length}</h2>
          </article>

          <article className="dashboard-stat-card">
            <div className="stat-icon">⇩</div>
            <p>Shared With Me</p>
            <h2>{loading ? "..." : sharedDocuments.length}</h2>
          </article>
        </section>

        {/* Recent Documents */}
        <section className="dashboard-recent">
          <div className="dashboard-section-header">
            <div>
              <h2>Recent Documents</h2>
              <p>Your recently uploaded files</p>
            </div>

            <Link to="/documents">View all</Link>
          </div>

          {loading ? (
            <p className="dashboard-empty">Loading documents...</p>
          ) : documents.length === 0 ? (
            <div className="dashboard-empty">
              <p>No documents uploaded yet.</p>
              <Link to="/documents/upload">
                Upload your first document
              </Link>
            </div>
          ) : (
            <div className="dashboard-table-wrapper">
              <table className="dashboard-table">
                <thead>
                  <tr>
                    <th>Document Name</th>
                    <th>File Type</th>
                    <th>File Size</th>
                    <th>Uploaded On</th>
                  </tr>
                </thead>

                <tbody>
                  {documents.map((document) => (
                    <tr key={document.documentId}>
                      <td>
                        <span className="dashboard-file-name">
                          {document.fileName}
                        </span>
                      </td>

                      <td>
                        <span className="file-type-badge">
                          {formatFileType(document.mimeType)}
                        </span>
                      </td>

                      <td>{formatSize(document.fileSizeBytes)}</td>

                      <td>{formatDateTime(document.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

                {/* Shared With Me */}
        <section className="dashboard-recent">
          <div className="dashboard-section-header">
            <div>
              <h2>Shared With Me</h2>
              <p>Documents other users have shared with you</p>
            </div>
          </div>

          {loading ? (
            <p className="dashboard-empty">Loading shared documents...</p>
          ) : sharedDocuments.length === 0 ? (
            <div className="dashboard-empty">
              <p>No documents have been shared with you yet.</p>
            </div>
          ) : (
            <div className="dashboard-table-wrapper">
              <table className="dashboard-table">
                <thead>
                  <tr>
                    <th>Document Name</th>
                    <th>Shared On</th>
                    <th>View Permission</th>
                    <th>Download Permission</th>
                    <th>Share Permission</th>
                    <th>Expiry</th>
                    <th>Actions</th>
                  </tr>
                </thead>

                <tbody>
                  {sharedDocuments.map((share) => (
                    <tr key={share.shareId}>
                      <td>
                        <span className="dashboard-file-name">
                          {share.documentName}
                        </span>
                      </td>

                      <td>{formatDateTime(share.createdAt)}</td>

                      <td>
                        <PermissionBadge allowed={share.canView} />
                      </td>

                      <td>
                        <PermissionBadge allowed={share.canDownload} />
                      </td>

                      <td>
                        <PermissionBadge allowed={share.canShare} />
                      </td>

                      <td>
                        {share.expiresAt
                          ? formatDateTime(share.expiresAt)
                          : "No expiry"}
                      </td>

                      <td>
                        <div className="shared-document-actions">
                          {share.canView && (
                            <button
                              className="dashboard-action-button"
                              onClick={() => handlePreview(share)}
                            >
                              Preview
                            </button>
                          )}

                          {share.canDownload && (
                            <button
                              className="dashboard-action-button"
                              onClick={() => handleDownload(share)}
                            >
                              Download
                            </button>
                          )}

                          {share.canShare && (
                            <button
                              className="dashboard-action-button"
                              onClick={() => openShareModal(share)}
                            >
                              Share
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* Recent Activity */}
        <section className="dashboard-recent">
          <div className="dashboard-section-header">
            <div>
              <h2>Recent Activity</h2>
              <p>Your latest account and document activities</p>
            </div>

            <Link to="/audit-logs">View All</Link>
          </div>

          {auditLogsLoading ? (
            <div className="dashboard-empty">
              Loading recent activity...
            </div>
          ) : recentAuditLogs.length === 0 ? (
            <div className="dashboard-empty">
              No recent activity found.
            </div>
          ) : (
            <div className="dashboard-table-wrapper">
              <table className="dashboard-table audit-logs-table">
                <thead>
                  <tr>
                    <th>Action</th>
                    <th>Resource</th>
                    <th>Outcome</th>
                    <th>Date & Time</th>
                  </tr>
                </thead>

                <tbody>
                  {recentAuditLogs.map((log) => (
                    <tr key={log.id}>
                      <td>
                        <span className="dashboard-file-name">
                          {log.action || "N/A"}
                        </span>
                      </td>

                      <td>
                        {log.resourceType || "N/A"}
                      </td>

                      <td>
                        <span
                          className={`audit-outcome ${getOutcomeClass(log.outcome)}`}
                        >
                          {log.outcome || "UNKNOWN"}
                        </span>
                      </td>

                      <td>{formatDateTime(log.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* Share Document Modal */}
        {shareDocument && (
          <div className="share-modal-overlay">
            <section
              className="share-modal"
              role="dialog"
              aria-modal="true"
              aria-labelledby="dashboard-share-modal-title"
            >
              <button
                type="button"
                className="share-modal-close"
                onClick={closeShareModal}
                disabled={sharing}
                aria-label="Close share dialog"
              >
                ×
              </button>

              <h2 id="dashboard-share-modal-title">Share Document</h2>
              <p>{shareDocument.fileName}</p>

              <form onSubmit={handleShare}>
                <label htmlFor="dashboard-share-email">
                  Recipient Email
                </label>

                <input
                  id="dashboard-share-email"
                  type="email"
                  value={shareEmail}
                  onChange={(event) => setShareEmail(event.target.value)}
                  placeholder="user@example.com"
                  required
                />

                <label className="share-checkbox">
                  <input
                    type="checkbox"
                    checked={canView}
                    onChange={(event) => setCanView(event.target.checked)}
                  />
                  Allow Viewing
                </label>

                <label className="share-checkbox">
                  <input
                    type="checkbox"
                    checked={canDownload}
                    onChange={(event) =>
                      setCanDownload(event.target.checked)
                    }
                  />
                  Allow Downloading
                </label>

                <label className="share-checkbox">
                  <input
                    type="checkbox"
                    checked={canShare}
                    onChange={(event) => setCanShare(event.target.checked)}
                  />
                  Allow Resharing
                </label>

                <label htmlFor="dashboard-share-expiry">
                  Expiry Date and Time (Optional)
                </label>

                <input
                  id="dashboard-share-expiry"
                  type="datetime-local"
                  value={expiresAt}
                  onChange={(event) => setExpiresAt(event.target.value)}
                />

                <button type="submit" disabled={sharing}>
                  {sharing ? "Sharing..." : "Share Document"}
                </button>
              </form>
            </section>
          </div>
        )}
      </main>
    </div>
  );
}

export default Dashboard;

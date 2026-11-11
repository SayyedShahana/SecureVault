
import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { auditLogApi } from "../services/api";
import "./Dashboard.css";
import "./Documents.css";
import "./AuditLogs.css";

function formatDateTime(date) {
  if (!date) return "N/A";

  const parsedDate = new Date(date);

  if (Number.isNaN(parsedDate.getTime())) {
    return "N/A";
  }

  return parsedDate.toLocaleString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: true,
  });
}

function formatLabel(value) {
  if (!value) return "N/A";

  return String(value)
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replace(/[_-]/g, " ")
    .toLowerCase()
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function formatAction(action) {
  const actions = {
    LOGIN: "Login",
    DOCUMENT_UPLOADED: "Document Uploaded",
    DOCUMENT_DOWNLOADED: "Document Downloaded",
    DOCUMENT_PREVIEWED: "Document Previewed",
    DOCUMENT_RENAMED: "Document Renamed",
    DOCUMENT_DELETED: "Document Deleted",
    DOCUMENT_SHARED: "Document Shared",
    SHARE_PERMISSION_UPDATED: "Sharing Permission Updated",
    DOCUMENT_SHARE_REVOKED: "Document Access Revoked",
  };

  const normalized = String(action || "").toUpperCase();

  return actions[normalized] || formatLabel(action);
}

function getReadableDetails(details) {
  if (!details) return {};

  if (typeof details === "object") {
    return details;
  }

  if (typeof details === "string") {
    try {
      const parsed = JSON.parse(details);

      if (parsed && typeof parsed === "object") {
        return parsed;
      }

      return { description: parsed };
    } catch {
      return { description: details };
    }
  }

  return { description: details };
}

function formatDetailValue(value) {
  if (value === null || value === undefined || value === "") {
    return "N/A";
  }

  if (typeof value === "boolean") {
    return value ? "Yes" : "No";
  }

  if (Array.isArray(value)) {
    return value.map(formatDetailValue).join(", ");
  }

  if (typeof value === "object") {
    return Object.entries(value)
      .map(([key, item]) => `${formatLabel(key)}: ${formatDetailValue(item)}`)
      .join(" | ");
  }

  return String(value);
}

function AuditLogs() {
  const { user, role, logout } = useAuth();
  const navigate = useNavigate();

  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedLog, setSelectedLog] = useState(null);

  useEffect(() => {
    const loadAuditLogs = async () => {
      try {
        setLoading(true);
        setError("");

        const response = await auditLogApi.getMyAuditLogs();

        setAuditLogs(response.data || []);
      } catch (err) {
        setError(err.message || "Unable to load audit logs.");
      } finally {
        setLoading(false);
      }
    };

    loadAuditLogs();
  }, []);

  const handleLogout = async () => {
    try {
      await logout();
    } finally {
      navigate("/login", { replace: true });
    }
  };

  const isLoginAction = (action) =>
    String(action || "").toUpperCase().includes("LOGIN");

  // Hide login activities only for normal users.
  const visibleLogs =
    role === "USER"
      ? auditLogs.filter((log) => !isLoginAction(log.action))
      : auditLogs;

  const getOutcomeClass = (outcome) => {
    return outcome?.toUpperCase() === "SUCCESS"
      ? "audit-outcome-success"
      : "audit-outcome-failed";
  };

  const renderDetailRows = (details, parentKey = "") => {
    const entries = Object.entries(details || {});

    if (entries.length === 0) {
      return (
        <p className="audit-modal-no-details">
          No additional details available.
        </p>
      );
    }

    return entries.map(([key, value]) => {
      const label = parentKey
        ? `${parentKey} / ${formatLabel(key)}`
        : formatLabel(key);

      if (
        value &&
        typeof value === "object" &&
        !Array.isArray(value)
      ) {
        return (
          <div className="audit-detail-group" key={label}>
            <strong>{formatLabel(key)}</strong>
            {renderDetailRows(value, label)}
          </div>
        );
      }

      return (
        <div className="audit-detail-row" key={label}>
          <span>{formatLabel(key)}</span>
          <strong>{formatDetailValue(value)}</strong>
        </div>
      );
    });
  };

  return (
    <div className="dashboard-layout">
      <aside className="dashboard-sidebar">
        <Link
          to={role === "ADMIN" ? "/admin" : "/dashboard"}
          className="dashboard-brand"
        >
          <span className="dashboard-brand-icon">S</span>
          SecureVault
        </Link>

        <nav className="dashboard-nav">
          <Link to={role === "ADMIN" ? "/admin" : "/dashboard"}>
            <span>▦</span> Dashboard
          </Link>

          {role === "USER" && (
            <>
              <Link to="/documents">
                <span>▤</span> My Documents
              </Link>

              <Link to="/documents/upload">
                <span>↑</span> Upload Document
              </Link>

              <Link to="/shared-by-me">
                <span>⇧</span> Shared By Me
              </Link>
            </>
          )}

          <Link to="/audit-logs" className="active">
            <span>▧</span> Activity History
          </Link>
        </nav>

        <button className="dashboard-logout" onClick={handleLogout}>
          ↪ Logout
        </button>
      </aside>

      <main className="dashboard-main">
        <header className="dashboard-header">
          <div>
            <h1>Activity History</h1>
            <p>
              View and track your account and document activities.
            </p>
          </div>

          <div className="dashboard-header-actions">
            <div className="dashboard-user-profile">
              <div className="dashboard-user-avatar">
                {(user?.email || "U").charAt(0).toUpperCase()}
              </div>

              <div className="dashboard-user-info">
                <strong>{user?.email || "User"}</strong>
                <span>{role || "USER"}</span>
              </div>
            </div>
          </div>
        </header>

        {error && <p className="dashboard-error">{error}</p>}

        <section className="dashboard-recent audit-logs-section">
          <div className="dashboard-section-header">
            <div>
              <h2>Recent Activities</h2>
              <p>
                {loading
                  ? "Loading activities..."
                  : `${visibleLogs.length} activity records found`}
              </p>
            </div>
          </div>

          {loading ? (
            <div className="dashboard-empty">
              Loading activity history...
            </div>
          ) : error ? (
            <div className="dashboard-empty">
              Unable to retrieve activity history.
            </div>
          ) : visibleLogs.length === 0 ? (
            <div className="dashboard-empty">
              No activity records found.
            </div>
          ) : (
            <div className="dashboard-table-wrapper">
              <table className="dashboard-table audit-logs-table">
                <thead>
                  <tr>
                    <th>Activity</th>
                    <th>Resource</th>
                    <th>Status</th>
                    <th>Date & Time</th>
                    <th>Details</th>
                  </tr>
                </thead>

                <tbody>
                  {visibleLogs.map((log) => {
                    const details = getReadableDetails(log.details);

                    const resourceName =
                      details.fileName ||
                      details.documentName ||
                      details.filename ||
                      log.resourceType;

                    return (
                      <tr key={log.id}>
                        <td>
                          <span className="audit-action-name">
                            {formatAction(log.action)}
                          </span>
                        </td>

                        <td>
                          {formatDetailValue(resourceName)}
                        </td>

                        <td>
                          <span
                            className={`audit-outcome ${getOutcomeClass(
                              log.outcome
                            )}`}
                          >
                            {formatLabel(log.outcome)}
                          </span>
                        </td>

                        <td>{formatDateTime(log.createdAt)}</td>

                        <td>
                          <button
                            type="button"
                            className="audit-view-details"
                            onClick={() => setSelectedLog(log)}
                          >
                            View Details
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </main>

      {selectedLog && (
        <div
          className="audit-modal-overlay"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              setSelectedLog(null);
            }
          }}
        >
          <div
            className="audit-details-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="audit-modal-title"
          >
            <div className="audit-modal-header">
              <div>
                <h2 id="audit-modal-title">Activity Details</h2>
                <p>Complete information about this activity.</p>
              </div>

              <button
                type="button"
                className="audit-modal-close"
                onClick={() => setSelectedLog(null)}
                aria-label="Close details"
              >
                ×
              </button>
            </div>

            <div className="audit-modal-content">
              <div className="audit-detail-row">
                <span>Activity</span>
                <strong>{formatAction(selectedLog.action)}</strong>
              </div>

              <div className="audit-detail-row">
                <span>Resource Type</span>
                <strong>{formatLabel(selectedLog.resourceType)}</strong>
              </div>

              <div className="audit-detail-row">
                <span>Resource ID</span>
                <strong>
                  {formatDetailValue(selectedLog.resourceId)}
                </strong>
              </div>

              <div className="audit-detail-row">
                <span>Outcome</span>
                <strong>
                  <span
                    className={`audit-outcome ${getOutcomeClass(
                      selectedLog.outcome
                    )}`}
                  >
                    {formatLabel(selectedLog.outcome)}
                  </span>
                </strong>
              </div>

              <div className="audit-detail-row">
                <span>Date & Time</span>
                <strong>{formatDateTime(selectedLog.createdAt)}</strong>
              </div>

              <div className="audit-modal-divider" />

              <h3>Additional Information</h3>

              {renderDetailRows(
                getReadableDetails(selectedLog.details)
              )}
            </div>

            <div className="audit-modal-footer">
              <button
                type="button"
                onClick={() => setSelectedLog(null)}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default AuditLogs;

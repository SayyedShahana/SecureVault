
import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { auditLogApi } from "../services/api";
import "./Dashboard.css";
import "./Documents.css";
import "./AuditLogs.css";

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
function displayValue(value) {
  if (value === null || value === undefined || value === "") {
    return "N/A";
  }

  if (typeof value === "object") {
    return JSON.stringify(value);
  }

  return String(value);
}
function AuditLogs() {
  const { user, role, logout } = useAuth();
  const navigate = useNavigate();

  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

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

  const getOutcomeClass = (outcome) => {
    if (outcome?.toUpperCase() === "SUCCESS") {
      return "audit-outcome-success";
    }

    return "audit-outcome-failed";
  };

  return (
    <div className="dashboard-layout">
      <aside className="dashboard-sidebar">
        <Link to={role === "ADMIN" ? "/admin" : "/dashboard"} className="dashboard-brand">
          <span className="dashboard-brand-icon">S</span>
          SecureVault
        </Link>

        <nav className="dashboard-nav">
          <Link to={role === "ADMIN" ? "/admin" : "/dashboard"}>
            <span>▦</span> Dashboard
          </Link>

          <Link to="/documents">
            <span>▤</span> My Documents
          </Link>

          <Link to="/documents/upload">
            <span>↑</span> Upload Document
          </Link>

          <Link to="/audit-logs" className="active">
            <span>▧</span> My Audit Logs
          </Link>
        </nav>

        <button className="dashboard-logout" onClick={handleLogout}>
          ↪ Logout
        </button>
      </aside>

      <main className="dashboard-main">
        <header className="dashboard-header">
          <div>
            <h1>My Audit Logs</h1>
            <p>Track your account activity and document-related actions.</p>
          </div>

          <div className="dashboard-header-actions">
            <div className="dashboard-user-profile">
              <div className="dashboard-user-avatar">
                {(user?.email || "U").charAt(0).toUpperCase()}
              </div>

              <div className="dashboard-user-info">
                <strong>{user?.email || "User"}</strong>
                <span>{role || user?.role || "USER"}</span>
              </div>
            </div>
          </div>
        </header>

        {error && <p className="dashboard-error">{error}</p>}

        <section className="dashboard-recent audit-logs-section">
          <div className="dashboard-section-header">
            <div>
              <h2>Activity History</h2>
              <p>
                {loading
                  ? "Loading your activity..."
                  : `${auditLogs.length} activity records found`}
              </p>
            </div>
          </div>

          {loading ? (
            <div className="dashboard-empty">
              Loading audit logs...
            </div>
          ) : error ? (
            <div className="dashboard-empty">
              Unable to retrieve your audit logs.
            </div>
          ) : auditLogs.length === 0 ? (
            <div className="dashboard-empty">
              No activity records found.
            </div>
          ) : (
            <div className="dashboard-table-wrapper">
              <table className="dashboard-table audit-logs-table">
                <thead>
                  <tr>
                    <th>Action</th>
                    <th>Resource Type</th>
                    <th>Resource ID</th>
                    <th>Outcome</th>
                    <th>Details</th>
                    <th>Date & Time</th>
                  </tr>
                </thead>

                <tbody>
                  {auditLogs.map((log) => (
  <tr key={log.id}>
    <td>
      <span className="dashboard-file-name">
        {displayValue(log.action)}
      </span>
    </td>

    <td>{displayValue(log.resourceType)}</td>

    <td className="audit-resource-id">
      {displayValue(log.resourceId)}
    </td>

    <td>
      <span
        className={`audit-outcome ${getOutcomeClass(log.outcome)}`}
      >
        {displayValue(log.outcome)}
      </span>
    </td>

    <td className="audit-details">
      {displayValue(log.details)}
    </td>

    <td>{displayValue(log.createdAt)}</td>
  </tr>
))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}

export default AuditLogs;

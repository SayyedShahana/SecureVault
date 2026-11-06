
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { adminApi } from "../services/api";
import { useAuth } from "../context/AuthContext";
import "./Dashboard.css";
import "../styles/AdminDashboard.css";

function formatDate(dateValue) {
  if (!dateValue) return "N/A";

  const date = new Date(dateValue);

  if (Number.isNaN(date.getTime())) return "N/A";

  return date.toLocaleString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function getInitials(name) {
  if (!name) return "A";

  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join("");
}

function AdminDashboard() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const [users, setUsers] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);

  const [activeTab, setActiveTab] = useState("overview");

  const [userSearch, setUserSearch] = useState("");
  const [auditSearch, setAuditSearch] = useState("");
  const [auditFilter, setAuditFilter] = useState("ALL");

  const [loading, setLoading] = useState(true);
  const [userError, setUserError] = useState("");
  const [auditError, setAuditError] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    let isMounted = true;

    async function fetchAdminData() {
      setLoading(true);
      setError("");
      setUserError("");
      setAuditError("");

      const [usersResult, logsResult] = await Promise.allSettled([
        adminApi.getUsers(),
        adminApi.getAuditLogs(),
      ]);

      if (!isMounted) return;

      if (usersResult.status === "fulfilled") {
        setUsers(usersResult.value.data || []);
      } else {
        setUserError(
          usersResult.reason?.message || "Unable to load users."
        );
      }

      if (logsResult.status === "fulfilled") {
        setAuditLogs(logsResult.value.data || []);
      } else {
        setAuditError(
          logsResult.reason?.message || "Unable to load audit logs."
        );
      }

      if (
        usersResult.status === "rejected" &&
        logsResult.status === "rejected"
      ) {
        setError("Unable to load Admin Dashboard data.");
      }

      setLoading(false);
    }

    fetchAdminData();

    return () => {
      isMounted = false;
    };
  }, []);

  const filteredUsers = useMemo(() => {
    const search = userSearch.trim().toLowerCase();

    if (!search) return users;

    return users.filter((item) =>
      [
        item.fullName,
        item.email,
        item.role,
        String(item.id),
      ]
        .filter(Boolean)
        .some((value) =>
          String(value).toLowerCase().includes(search)
        )
    );
  }, [users, userSearch]);

  const filteredAuditLogs = useMemo(() => {
    const search = auditSearch.trim().toLowerCase();

    return auditLogs.filter((log) => {
      const matchesFilter =
        auditFilter === "ALL" ||
        String(log.outcome || "").toUpperCase() === auditFilter;

      const searchableValues = [
        log.action,
        log.resourceType,
        log.resourceId,
        log.outcome,
        log.ipAddress,
        log.actorUserId,
        JSON.stringify(log.details || {}),
      ];

      const matchesSearch =
        !search ||
        searchableValues
          .filter((value) => value !== null && value !== undefined)
          .some((value) =>
            String(value).toLowerCase().includes(search)
          );

      return matchesFilter && matchesSearch;
    });
  }, [auditLogs, auditSearch, auditFilter]);

  const adminCount = users.filter(
    (item) => item.role?.toUpperCase() === "ADMIN"
  ).length;

  const successCount = auditLogs.filter(
    (log) => log.outcome?.toUpperCase() === "SUCCESS"
  ).length;

  const failureCount = auditLogs.filter(
    (log) => log.outcome?.toUpperCase() === "FAILURE"
  ).length;

  const recentLogs = [...auditLogs]
    .sort(
      (a, b) =>
        new Date(b.createdAt || 0) -
        new Date(a.createdAt || 0)
    )
    .slice(0, 5);

  const handleLogout = async () => {
    try {
      await logout();
      navigate("/login", { replace: true });
    } catch (err) {
      setError(err.message || "Unable to log out.");
    }
  };

  const renderAuditTable = (logs) => (
    <div className="dashboard-table-wrapper admin-table-wrapper">
      <table className="dashboard-table admin-table">
        <thead>
          <tr>
            <th>Action</th>
            <th>Resource</th>
            <th>User ID</th>
            <th>Outcome</th>
            <th>IP Address</th>
            <th>Date & Time</th>
          </tr>
        </thead>

        <tbody>
          {logs.length === 0 ? (
            <tr>
              <td colSpan="6" className="admin-empty-cell">
                No audit records found.
              </td>
            </tr>
          ) : (
            logs.map((log) => (
              <tr key={log.id}>
                <td>
                  <strong>{log.action || "N/A"}</strong>
                </td>

                <td>
                  {log.resourceType || "N/A"}
                  {log.resourceId && (
                    <small className="admin-muted">
                      ID: {log.resourceId}
                    </small>
                  )}
                </td>

                <td>{log.actorUserId ?? "N/A"}</td>

                <td>
                  <span
                    className={`admin-status ${
                      log.outcome?.toUpperCase() === "SUCCESS"
                        ? "status-success"
                        : log.outcome?.toUpperCase() === "FAILURE"
                          ? "status-failure"
                          : "status-neutral"
                    }`}
                  >
                    {log.outcome || "N/A"}
                  </span>
                </td>

                <td>{log.ipAddress || "N/A"}</td>

                <td>{formatDate(log.createdAt)}</td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );

  return (
    <div className="dashboard-layout admin-layout">
      <aside className="dashboard-sidebar admin-sidebar">
        <Link to="/admin" className="dashboard-brand">
          <span className="dashboard-brand-icon">S</span>
          SecureVault
        </Link>

        <nav className="dashboard-nav admin-navigation">
          <button
            className={activeTab === "overview" ? "active" : ""}
            onClick={() => setActiveTab("overview")}
          >
            <span>▦</span>
            Overview
          </button>

          <button
            className={activeTab === "users" ? "active" : ""}
            onClick={() => setActiveTab("users")}
          >
            <span>♙</span>
            User Management
          </button>

          <button
            className={activeTab === "audit" ? "active" : ""}
            onClick={() => setActiveTab("audit")}
          >
            <span>≡</span>
            Audit Logs
          </button>
        </nav>

        <div className="admin-sidebar-profile">
          <div className="admin-avatar">
            {getInitials(user?.email)}
          </div>

          <div className="admin-profile-info">
            <strong>Administrator</strong>
            <span>{user?.email || "Admin account"}</span>
          </div>
        </div>

        <button
          className="dashboard-logout"
          onClick={handleLogout}
        >
          ↪ Logout
        </button>
      </aside>

      <main className="dashboard-main admin-main">
        <header className="dashboard-header">
          <div>
            <h1>
              {activeTab === "overview"
                ? "Admin Dashboard"
                : activeTab === "users"
                  ? "User Management"
                  : "Audit Logs"}
            </h1>

            <p>
              {activeTab === "overview"
                ? "Monitor users and security activities."
                : activeTab === "users"
                  ? "View and manage registered account information."
                  : "Monitor recorded system activities and events."}
            </p>
          </div>

          <div className="admin-header-profile">
            <div className="admin-avatar">
              {getInitials(user?.email)}
            </div>
            <div>
              <strong>Administrator</strong>
              <span>ADMIN</span>
            </div>
          </div>
        </header>

        {error && (
          <div className="dashboard-error">
            {error}
            <button
              className="admin-retry-button"
              onClick={() => window.location.reload()}
            >
              Retry
            </button>
          </div>
        )}

        {loading ? (
          <div className="admin-loading">
            Loading Admin Dashboard...
          </div>
        ) : (
          <>
            {activeTab === "overview" && (
              <>
                <section className="dashboard-stats admin-stats">
                  <article className="dashboard-stat-card">
                    <div className="stat-icon">♙</div>
                    <p>Total Users</p>
                    <h2>{users.length}</h2>
                  </article>

                  <article className="dashboard-stat-card">
                    <div className="stat-icon">♛</div>
                    <p>Administrators</p>
                    <h2>{adminCount}</h2>
                  </article>

                  <article className="dashboard-stat-card">
                    <div className="stat-icon">≡</div>
                    <p>Audit Events</p>
                    <h2>{auditLogs.length}</h2>
                  </article>
                </section>

                <section className="dashboard-recent">
                  <div className="dashboard-section-header">
                    <div>
                      <h2>Recent Audit Activity</h2>
                      <p>Latest recorded system events</p>
                    </div>

                    <button
                      className="admin-text-button"
                      onClick={() => setActiveTab("audit")}
                    >
                      View all
                    </button>
                  </div>

                  {auditError ? (
                    <p className="dashboard-error">{auditError}</p>
                  ) : (
                    renderAuditTable(recentLogs)
                  )}
                </section>

                <section className="dashboard-recent">
                  <div className="dashboard-section-header">
                    <div>
                      <h2>Security Summary</h2>
                      <p>Recorded audit outcomes</p>
                    </div>
                  </div>

                  <div className="admin-summary-grid">
                    <div className="admin-summary-item">
                      <span>Successful events</span>
                      <strong className="summary-success">
                        {successCount}
                      </strong>
                    </div>

                    <div className="admin-summary-item">
                      <span>Failed events</span>
                      <strong className="summary-failure">
                        {failureCount}
                      </strong>
                    </div>

                    <div className="admin-summary-item">
                      <span>Total audit records</span>
                      <strong>{auditLogs.length}</strong>
                    </div>
                  </div>
                </section>
              </>
            )}

            {activeTab === "users" && (
              <section className="dashboard-recent">
                <div className="dashboard-section-header admin-list-header">
                  <div>
                    <h2>Registered Users</h2>
                    <p>
                      {filteredUsers.length} of {users.length} users
                    </p>
                  </div>

                  <input
                    type="search"
                    className="admin-search"
                    placeholder="Search name, email or role..."
                    value={userSearch}
                    onChange={(event) =>
                      setUserSearch(event.target.value)
                    }
                  />
                </div>

                {userError ? (
                  <p className="dashboard-error">{userError}</p>
                ) : (
                  <div className="dashboard-table-wrapper">
                    <table className="dashboard-table">
                      <thead>
                        <tr>
                          <th>User</th>
                          <th>User ID</th>
                          <th>Role</th>
                        </tr>
                      </thead>

                      <tbody>
                        {filteredUsers.length === 0 ? (
                          <tr>
                            <td colSpan="3" className="admin-empty-cell">
                              No users found.
                            </td>
                          </tr>
                        ) : (
                          filteredUsers.map((item) => (
                            <tr key={item.id}>
                              <td>
                                <div className="admin-user-cell">
                                  <div className="admin-avatar">
                                    {getInitials(item.fullName)}
                                  </div>

                                  <div>
                                    <strong>{item.fullName}</strong>
                                    <small>{item.email}</small>
                                  </div>
                                </div>
                              </td>

                              <td>{item.id}</td>

                              <td>
                                <span
                                  className={`admin-role ${
                                    item.role?.toUpperCase() === "ADMIN"
                                      ? "role-admin"
                                      : "role-user"
                                  }`}
                                >
                                  {item.role || "N/A"}
                                </span>
                              </td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>
            )}

            {activeTab === "audit" && (
              <section className="dashboard-recent">
                <div className="dashboard-section-header admin-list-header">
                  <div>
                    <h2>System Audit Logs</h2>
                    <p>
                      {filteredAuditLogs.length} of {auditLogs.length} events
                    </p>
                  </div>

                  <div className="admin-audit-controls">
                    <input
                      type="search"
                      className="admin-search"
                      placeholder="Search audit events..."
                      value={auditSearch}
                      onChange={(event) =>
                        setAuditSearch(event.target.value)
                      }
                    />

                    <select
                      className="admin-filter"
                      value={auditFilter}
                      onChange={(event) =>
                        setAuditFilter(event.target.value)
                      }
                    >
                      <option value="ALL">All outcomes</option>
                      <option value="SUCCESS">Success</option>
                      <option value="FAILURE">Failure</option>
                    </select>
                  </div>
                </div>

                {auditError ? (
                  <p className="dashboard-error">{auditError}</p>
                ) : (
                  renderAuditTable(filteredAuditLogs)
                )}
              </section>
            )}
          </>
        )}
      </main>
    </div>
  );
}

export default AdminDashboard;

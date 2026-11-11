import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { adminApi } from "../services/api";
import { useAuth } from "../context/AuthContext";
import "./Dashboard.css";
import "../styles/AdminDashboard.css";

const AUDIT_PAGE_SIZE = 10;

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

  const profileMenuRef = useRef(null);

  const [users, setUsers] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [overviewAuditLogs, setOverviewAuditLogs] = useState([]);

  const [auditSummary, setAuditSummary] = useState({
    successfulEvents: 0,
    failedEvents: 0,
    totalAuditRecords: 0,
  });

  const [auditPage, setAuditPage] = useState(0);
  const [auditTotalPages, setAuditTotalPages] = useState(0);
  const [auditTotalElements, setAuditTotalElements] = useState(0);

  const [loading, setLoading] = useState(true);
  const [auditLoading, setAuditLoading] = useState(false);

  const [userError, setUserError] = useState("");
  const [auditError, setAuditError] = useState("");
  const [error, setError] = useState("");

  const [activeTab, setActiveTab] = useState("overview");

  const [userSearch, setUserSearch] = useState("");
  const [auditSearch, setAuditSearch] = useState("");
  const [auditFilter, setAuditFilter] = useState("ALL");

  const [profileMenuOpen, setProfileMenuOpen] = useState(false);

  // Close profile dropdown when clicking outside or pressing Escape.
  useEffect(() => {
    function handleOutsideClick(event) {
      if (
        profileMenuRef.current &&
        !profileMenuRef.current.contains(event.target)
      ) {
        setProfileMenuOpen(false);
      }
    }

    function handleEscape(event) {
      if (event.key === "Escape") {
        setProfileMenuOpen(false);
      }
    }

    document.addEventListener("mousedown", handleOutsideClick);
    document.addEventListener("keydown", handleEscape);

    return () => {
      document.removeEventListener("mousedown", handleOutsideClick);
      document.removeEventListener("keydown", handleEscape);
    };
  }, []);

  // Load registered users.
  useEffect(() => {
    let isMounted = true;

    async function fetchUsers() {
      setLoading(true);
      setUserError("");
      setError("");

      try {
        const result = await adminApi.getUsers();

        if (isMounted) {
          setUsers(result?.data || []);
        }
      } catch (err) {
        if (isMounted) {
          setUserError(err.message || "Unable to load users.");
          setError("Unable to load Admin Dashboard data.");
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    fetchUsers();

    return () => {
      isMounted = false;
    };
  }, []);

  // Load paginated audit logs.
  useEffect(() => {
    let isMounted = true;

    async function fetchAuditLogs() {
      setAuditLoading(true);
      setAuditError("");

      try {
        const result = await adminApi.getAuditLogs(
          auditPage,
          AUDIT_PAGE_SIZE
        );

        if (!isMounted) return;

        // ApiResponse.data contains the Spring Page object.
        const pageData = result?.data || {};
        const records = pageData.content || [];

        setAuditLogs(records);
        setAuditTotalPages(pageData.totalPages || 0);
        setAuditTotalElements(pageData.totalElements || 0);

        // Keep the first page for dashboard recent activity.
        if (auditPage === 0) {
          setOverviewAuditLogs(records);
        }
      } catch (err) {
        if (isMounted) {
          setAuditError(err.message || "Unable to load audit logs.");
          setAuditLogs([]);
        }
      } finally {
        if (isMounted) {
          setAuditLoading(false);
        }
      }
    }

    fetchAuditLogs();

    return () => {
      isMounted = false;
    };
  }, [auditPage]);

  // Load overall audit summary across all database records.
  useEffect(() => {
    let isMounted = true;

    async function fetchAuditSummary() {
      try {
        const result = await adminApi.getAuditLogSummary();

        if (isMounted) {
          setAuditSummary(
            result?.data || {
              successfulEvents: 0,
              failedEvents: 0,
              totalAuditRecords: 0,
            }
          );
        }
      } catch (err) {
        console.error("Unable to load audit summary:", err);
      }
    }

    fetchAuditSummary();

    return () => {
      isMounted = false;
    };
  }, []);

  // Search registered users.
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

  // Search audit logs and filter by outcome.
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
          .filter(
            (value) => value !== null && value !== undefined
          )
          .some((value) =>
            String(value).toLowerCase().includes(search)
          );

      return matchesFilter && matchesSearch;
    });
  }, [auditLogs, auditSearch, auditFilter]);

  const adminCount = users.filter(
    (item) => item.role?.toUpperCase() === "ADMIN"
  ).length;

  const recentLogs = [...overviewAuditLogs]
    .sort(
      (a, b) =>
        new Date(b.createdAt || 0) -
        new Date(a.createdAt || 0)
    )
    .slice(0, 5);

  // Logout from the existing AuthContext.
  const handleLogout = async () => {
    setProfileMenuOpen(false);
    setError("");

    try {
      await logout();
      navigate("/login", { replace: true });
    } catch (err) {
      setError(err.message || "Unable to log out.");
    }
  };

  // Reusable audit table.
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
            <th>Date &amp; Time</th>
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
      {/* SIDEBAR */}
      <aside className="dashboard-sidebar admin-sidebar">
        <Link to="/admin" className="dashboard-brand">
          <span className="dashboard-brand-icon">S</span>
          SecureVault
        </Link>

        <nav className="dashboard-nav admin-navigation">
          <button
            type="button"
            className={activeTab === "overview" ? "active" : ""}
            onClick={() => setActiveTab("overview")}
          >
            <span>▦</span>
            Overview
          </button>

          <button
            type="button"
            className={activeTab === "users" ? "active" : ""}
            onClick={() => setActiveTab("users")}
          >
            <span>♙</span>
            User Management
          </button>

          <button
            type="button"
            className={activeTab === "audit" ? "active" : ""}
            onClick={() => setActiveTab("audit")}
          >
            <span>≡</span>
            Audit Logs
          </button>
        </nav>
      </aside>

      {/* MAIN CONTENT */}
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

          {/* ADMIN PROFILE DROPDOWN */}
          <div
            className="admin-profile-menu-wrapper"
            ref={profileMenuRef}
          >
            <button
              type="button"
              className="admin-profile-trigger"
              onClick={() =>
                setProfileMenuOpen((current) => !current)
              }
              aria-expanded={profileMenuOpen}
              aria-haspopup="true"
              aria-label="Open administrator menu"
            >
              <div className="admin-avatar">
                {getInitials(user?.email)}
              </div>

              <div className="admin-header-profile-text">
                <strong>Administrator</strong>
                <span>ADMIN</span>
              </div>

              <span
                className={`admin-profile-chevron ${
                  profileMenuOpen ? "open" : ""
                }`}
              >
                ▾
              </span>
            </button>

            {profileMenuOpen && (
              <div className="admin-profile-dropdown">
                <div className="admin-dropdown-account">
                  <strong>Administrator</strong>
                  <span>{user?.email || "Admin account"}</span>
                </div>

                <div className="admin-dropdown-divider" />

                <button
                  type="button"
                  className="admin-dropdown-item"
                  disabled
                  title="Profile page is not available yet"
                >
                  <span>♙</span>
                  My Profile
                </button>

                <button
                  type="button"
                  className="admin-dropdown-item admin-dropdown-logout"
                  onClick={handleLogout}
                >
                  <span>↪</span>
                  Logout
                </button>
              </div>
            )}
          </div>
        </header>

        {error && (
          <div className="dashboard-error">
            {error}

            <button
              type="button"
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
            {/* OVERVIEW */}
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
                    <h2>{auditTotalElements}</h2>
                  </article>
                </section>

                <section className="dashboard-recent">
                  <div className="dashboard-section-header">
                    <div>
                      <h2>Recent Audit Activity</h2>
                      <p>Latest recorded system events</p>
                    </div>

                    <button
                      type="button"
                      className="admin-text-button"
                      onClick={() => setActiveTab("audit")}
                    >
                      View all
                    </button>
                  </div>

                  {auditError ? (
                    <p className="dashboard-error">
                      {auditError}
                    </p>
                  ) : auditLoading && overviewAuditLogs.length === 0 ? (
                    <div className="admin-loading">
                      Loading recent activity...
                    </div>
                  ) : (
                    renderAuditTable(recentLogs)
                  )}
                </section>

                <section className="dashboard-recent">
                  <div className="dashboard-section-header">
                    <div>
                      <h2>Security Summary</h2>
                      <p>
                        Recorded outcomes across all audit records
                      </p>
                    </div>
                  </div>

                  <div className="admin-summary-grid">
                    <div className="admin-summary-item">
                      <span>Successful events</span>
                      <strong className="summary-success">
                        {auditSummary.successfulEvents}
                      </strong>
                    </div>

                    <div className="admin-summary-item">
                      <span>Failed events</span>
                      <strong className="summary-failure">
                        {auditSummary.failedEvents}
                      </strong>
                    </div>

                    <div className="admin-summary-item">
                      <span>Total audit records</span>
                      <strong>{auditSummary.totalAuditRecords}</strong>
                    </div>
                  </div>
                </section>
              </>
            )}

            {/* USER MANAGEMENT */}
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
                            <td
                              colSpan="3"
                              className="admin-empty-cell"
                            >
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

            {/* AUDIT LOGS */}
            {activeTab === "audit" && (
              <section className="dashboard-recent">
                <div className="dashboard-section-header admin-list-header">
                  <div>
                    <h2>System Audit Logs</h2>
                    <p>
                      {filteredAuditLogs.length} shown on this page,{" "}
                      {auditTotalElements} total events
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
                  <p className="dashboard-error">
                    {auditError}
                  </p>
                ) : auditLoading ? (
                  <div className="admin-loading">
                    Loading audit logs...
                  </div>
                ) : (
                  <>
                    {renderAuditTable(filteredAuditLogs)}

                    <div className="admin-pagination">
                      <button
                        type="button"
                        className="admin-retry-button"
                        onClick={() =>
                          setAuditPage((current) =>
                            Math.max(0, current - 1)
                          )
                        }
                        disabled={auditPage <= 0}
                      >
                        Previous
                      </button>

                      <span>
                        Page{" "}
                        {auditTotalPages === 0 ? 0 : auditPage + 1}{" "}
                        of {auditTotalPages}
                      </span>

                      <button
                        type="button"
                        className="admin-retry-button"
                        onClick={() =>
                          setAuditPage((current) =>
                            Math.min(
                              auditTotalPages - 1,
                              current + 1
                            )
                          )
                        }
                        disabled={
                          auditTotalPages === 0 ||
                          auditPage >= auditTotalPages - 1
                        }
                      >
                        Next
                      </button>
                    </div>
                  </>
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

import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { documentApi } from "../services/api";
import "./SharedByMe.css";

function formatDateTime(value) {
  if (!value) {
    return "N/A";
  }

  return new Date(value).toLocaleString("en-IN");
}

function SharedByMe() {
  const [shares, setShares] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [updatingPermission, setUpdatingPermission] = useState(null);
  const [revokingShareId, setRevokingShareId] = useState(null);

  async function fetchSharedDocuments() {
    try {
      setLoading(true);
      setError("");

      const response = await documentApi.getSharedByMe();
      setShares(response.data || []);
    } catch (err) {
      setError(err.message || "Unable to load shared documents.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchSharedDocuments();
  }, []);

  async function handlePermissionChange(share, permissionKey, checked) {
    const permissionData = {
      canView: share.canView,
      canDownload: share.canDownload,
      canShare: share.canShare,
      [permissionKey]: checked,
    };

    try {
      setUpdatingPermission(`${share.shareId}-${permissionKey}`);
      setError("");

      await documentApi.updateSharePermission(
        share.documentId,
        share.shareId,
        permissionData
      );

      setShares((previousShares) =>
        previousShares.map((item) =>
          item.shareId === share.shareId
            ? { ...item, ...permissionData }
            : item
        )
      );
    } catch (err) {
      setError(err.message || "Unable to update permission.");
    } finally {
      setUpdatingPermission(null);
    }
  }

  async function handleRevoke(share) {
    const confirmed = window.confirm(
      `Are you sure you want to revoke all access for ${share.sharedWithEmail} to ${share.documentName}?`
    );

    if (!confirmed) {
      return;
    }

    try {
      setRevokingShareId(share.shareId);
      setError("");

      await documentApi.revokeShare(share.documentId, share.shareId);

      setShares((previousShares) =>
        previousShares.filter(
          (item) => item.shareId !== share.shareId
        )
      );
    } catch (err) {
      setError(err.message || "Unable to revoke document access.");
    } finally {
      setRevokingShareId(null);
    }
  }

  function renderPermission(share, permissionKey, label) {
    const isUpdating =
      updatingPermission === `${share.shareId}-${permissionKey}`;

    return (
      <label className="shared-by-me-permission-item">
        <input
          type="checkbox"
          checked={Boolean(share[permissionKey])}
          disabled={
            updatingPermission !== null ||
            revokingShareId === share.shareId
          }
          onChange={(event) =>
            handlePermissionChange(
              share,
              permissionKey,
              event.target.checked
            )
          }
        />

        <span className="shared-by-me-permission-label">
          {label}
        </span>

        <span
          className={
            share[permissionKey]
              ? "shared-by-me-status allowed"
              : "shared-by-me-status not-allowed"
          }
        >
          {share[permissionKey] ? "Allowed" : "Not Allowed"}
        </span>

        {isUpdating && (
          <span className="shared-by-me-updating">
            Updating...
          </span>
        )}
      </label>
    );
  }

  return (
    <div className="shared-by-me-page">
      <Link to="/dashboard" className="shared-by-me-back">
        ← Back to Dashboard
      </Link>

      <div className="shared-by-me-header">
        <div>
          <h1>Shared By Me</h1>
          <p>Manage documents and individual permissions shared with other users.</p>
        </div>
      </div>

      {error && (
        <div className="shared-by-me-error">
          {error}
        </div>
      )}

      {loading ? (
        <div className="shared-by-me-message">
          Loading shared documents...
        </div>
      ) : shares.length === 0 ? (
        <div className="shared-by-me-empty">
          <h2>No Shared Documents</h2>
          <p>
            You have not shared any documents with other users yet.
          </p>
          <Link to="/documents">Go to Documents</Link>
        </div>
      ) : (
        <>
          <div className="shared-by-me-summary">
            <strong>Total Active Shares:</strong> {shares.length}
          </div>

          <div className="shared-by-me-table-wrapper">
            <table className="shared-by-me-table">
              <thead>
                <tr>
                  <th>Document Name</th>
                  <th>Shared With</th>
                  <th>Preview</th>
                  <th>Download</th>
                  <th>Share</th>
                  <th>Shared On</th>
                  <th>Expires At</th>
                  <th>Actions</th>
                </tr>
              </thead>

              <tbody>
                {shares.map((share) => (
                  <tr key={share.shareId}>
                    <td>{share.documentName}</td>
                    <td>{share.sharedWithEmail}</td>

                    <td>
                      {renderPermission(share, "canView", "Preview")}
                    </td>

                    <td>
                      {renderPermission(
                        share,
                        "canDownload",
                        "Download"
                      )}
                    </td>

                    <td>
                      {renderPermission(share, "canShare", "Share")}
                    </td>

                    <td>{formatDateTime(share.createdAt)}</td>
                    <td>{formatDateTime(share.expiresAt)}</td>

                    <td>
                      <div className="shared-by-me-actions">
                        <button
                          type="button"
                          className="shared-by-me-revoke-btn"
                          disabled={
                            revokingShareId === share.shareId ||
                            updatingPermission !== null
                          }
                          onClick={() => handleRevoke(share)}
                        >
                          {revokingShareId === share.shareId
                            ? "Revoking..."
                            : "Revoke Access"}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}

export default SharedByMe;

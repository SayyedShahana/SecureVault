
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { documentApi } from "../services/api";
import "./Documents.css";

function Documents() {
  const [documents, setDocuments] = useState([]);
  const [search, setSearch] = useState("");
  const [appliedSearch, setAppliedSearch] = useState("");
  const [mimeType, setMimeType] = useState("");
  const [sortBy, setSortBy] = useState("createdAt");
  const [direction, setDirection] = useState("desc");

  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);

  // Document sharing states
  const [shareDocument, setShareDocument] = useState(null);
  const [shareEmail, setShareEmail] = useState("");
  const [canView, setCanView] = useState(true);
  const [canDownload, setCanDownload] = useState(false);
  const [canShare, setCanShare] = useState(false);
  const [expiresAt, setExpiresAt] = useState("");
  const [sharing, setSharing] = useState(false);

  // Fetch documents
  useEffect(() => {
    let cancelled = false;

    const fetchDocuments = async () => {
      try {
        setLoading(true);
        setError("");

        const response = await documentApi.getDocuments({
          search: appliedSearch,
          mimeType,
          sortBy,
          direction,
          page,
          size: pageSize,
        });

        if (cancelled) return;

        const result = response.data;

        setDocuments(result.content || []);
        setTotalPages(result.totalPages || 0);
        setTotalElements(result.totalElements || 0);
      } catch (err) {
        if (!cancelled) {
          setError(err.message || "Unable to load documents.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    fetchDocuments();

    return () => {
      cancelled = true;
    };
  }, [
    appliedSearch,
    mimeType,
    sortBy,
    direction,
    page,
    pageSize,
    refreshKey,
  ]);

  // Search documents
  const handleSearch = (event) => {
    event.preventDefault();
    setPage(0);
    setAppliedSearch(search.trim());
  };

  // Filter documents
  const handleFilterChange = (value) => {
    setPage(0);
    setMimeType(value);
  };

  // Change sorting field
  const handleSortChange = (value) => {
    setPage(0);
    setSortBy(value);

    // Default direction for the selected field
    setDirection(value === "createdAt" ? "desc" : "asc");
  };

  // Toggle sorting direction using the arrow button
  const handleDirectionToggle = () => {
    setPage(0);
    setDirection((current) =>
      current === "asc" ? "desc" : "asc"
    );
  };

  const sortDescription =
    sortBy === "createdAt"
      ? direction === "desc"
        ? "Newest first"
        : "Oldest first"
      : direction === "asc"
        ? "A to Z"
        : "Z to A";

  // Download document
  const handleDownload = async (document) => {
    try {
      const response = await documentApi.downloadDocument(
        document.documentId
      );

      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = window.document.createElement("a");

      link.href = url;
      link.download = document.fileName;

      window.document.body.appendChild(link);
      link.click();
      link.remove();

      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err.message || "Document download failed.");
    }
  };

  // Preview document
  const handlePreview = async (document) => {
    try {
      const response = await documentApi.previewDocument(
        document.documentId
      );

      const blob = await response.blob();
      const url = URL.createObjectURL(blob);

      window.open(url, "_blank", "noopener,noreferrer");

      setTimeout(() => URL.revokeObjectURL(url), 60000);
    } catch (err) {
      setError(err.message || "Document preview failed.");
    }
  };

  // Rename document
  const handleRename = async (document) => {
    const newFileName = window.prompt(
      "Enter the new document name:",
      document.fileName
    );

    if (!newFileName || newFileName.trim() === document.fileName) {
      return;
    }

    try {
      await documentApi.renameDocument(
        document.documentId,
        newFileName.trim()
      );

      setRefreshKey((value) => value + 1);
    } catch (err) {
      setError(err.message || "Document rename failed.");
    }
  };

  // Delete document
  const handleDelete = async (document) => {
    const confirmed = window.confirm(
      `Are you sure you want to delete "${document.fileName}"?`
    );

    if (!confirmed) return;

    try {
      await documentApi.deleteDocument(document.documentId);
      setRefreshKey((value) => value + 1);
    } catch (err) {
      setError(err.message || "Document deletion failed.");
    }
  };

  // Open share modal
  const openShareModal = (document) => {
    setError("");
    setShareDocument(document);
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
    setError("");

    if (!shareDocument || !shareEmail.trim()) {
      setError("Please enter the recipient's email.");
      return;
    }

    if (!canView && !canDownload && !canShare) {
      setError("Please select at least one permission.");
      return;
    }

    if (
      expiresAt &&
      new Date(expiresAt).getTime() <= Date.now()
    ) {
      setError("Expiry date must be in the future.");
      return;
    }

    try {
      setSharing(true);

      await documentApi.shareDocument(
        shareDocument.documentId,
        {
          email: shareEmail.trim(),
          canView,
          canDownload,
          canShare,
          expiresAt: expiresAt
            ? new Date(expiresAt).toISOString()
            : null,
        }
      );

      setShareDocument(null);
      setShareEmail("");
      setCanView(true);
      setCanDownload(false);
      setCanShare(false);
      setExpiresAt("");

      window.alert("Document shared successfully.");
    } catch (err) {
      setError(err.message || "Unable to share document.");
    } finally {
      setSharing(false);
    }
  };

  // Format file size
  const formatSize = (bytes) => {
    if (bytes < 1024) return `${bytes} B`;

    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(2)} KB`;
    }

    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  };

  // Format upload date and time
  const formatDate = (date) => {
    if (!date) return "N/A";

    return new Date(date).toLocaleString("en-IN", {
      day: "2-digit",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      hour12: true,
    });
  };

  return (
    <main className="documents-page">
      {/* Header */}
      <header className="documents-header">
        <div>
          <Link to="/dashboard" className="documents-back">
            ← Dashboard
          </Link>

          <h1>My Documents</h1>
          <p>Manage all your securely stored documents.</p>
        </div>

        <Link
          to="/documents/upload"
          className="documents-upload-btn"
        >
          + Upload Document
        </Link>
      </header>

      {/* Document Summary */}
      <section className="documents-summary">
        <strong>Total Documents: {totalElements}</strong>
      </section>

      {/* Search, Filter and Sorting */}
      <section className="documents-toolbar">
        {/* Search Documents */}
        <div className="documents-toolbar-group">
          <label htmlFor="documentSearch">Search Documents</label>

          <form onSubmit={handleSearch} className="documents-search">
            <input
              id="documentSearch"
              type="search"
              placeholder="Enter filename..."
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />

            <button type="submit">Search</button>
          </form>
        </div>

        {/* Filter Documents */}
        <div className="documents-toolbar-group">
          <label htmlFor="documentFilter">Filter Documents</label>

          <select
            id="documentFilter"
            value={mimeType}
            onChange={(event) =>
              handleFilterChange(event.target.value)
            }
          >
            <option value="">All file types</option>
            <option value="application/pdf">PDF</option>
            <option value="image/png">PNG</option>
            <option value="image/jpeg">JPG / JPEG</option>
            <option value="application/vnd.openxmlformats-officedocument.wordprocessingml.document">
              DOCX
            </option>
            <option value="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet">
              XLSX
            </option>
            <option value="text/plain">TXT</option>
          </select>
        </div>

        {/* Sort Documents */}
        <div className="documents-toolbar-group documents-sort-group">
          <label htmlFor="documentSort">Sort Documents</label>

          <div className="documents-sort-controls">
            <select
              id="documentSort"
              value={sortBy}
              onChange={(event) =>
                handleSortChange(event.target.value)
              }
              aria-label="Sort documents by"
            >
              <option value="createdAt">Upload Date</option>
              <option value="fileName">File Name</option>
            </select>

            <button
              type="button"
              className="documents-sort-direction"
              onClick={handleDirectionToggle}
              aria-label={`Change sort direction. Current order: ${sortDescription}`}
              title={`Current order: ${sortDescription}. Click to reverse.`}
            >
              <span aria-hidden="true">
                {direction === "asc" ? "↑" : "↓"}
              </span>
            </button>
          </div>

          <span className="documents-sort-description">
            {sortDescription}
          </span>
        </div>
      </section>

      {/* Error Message */}
      {error && (
        <p className="documents-error" role="alert">
          {error}
        </p>
      )}

      {/* Documents List */}
      {loading ? (
        <p className="documents-message">
          Loading documents...
        </p>
      ) : documents.length === 0 ? (
        <section className="documents-empty">
          <h2>No documents found</h2>

          <p>
            Upload a document or change your search and filters.
          </p>

          <Link to="/documents/upload">
            Upload your first document
          </Link>
        </section>
      ) : (
        <>
          <div className="documents-table-wrapper">
            <table className="documents-table">
              <thead>
                <tr>
                  <th>File Name</th>
                  <th>Type</th>
                  <th>Size</th>
                  <th>Uploaded</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>

              <tbody>
                {documents.map((document) => (
                  <tr key={document.documentId}>
                    <td>{document.fileName}</td>
                    <td>{document.mimeType}</td>
                    <td>{formatSize(document.fileSizeBytes)}</td>
                    <td>{formatDate(document.createdAt)}</td>
                    <td>{document.status}</td>

                    <td>
                      <div className="documents-actions">
                        <button
                          type="button"
                          onClick={() => openShareModal(document)}
                        >
                          Share
                        </button>

                        <button
                          type="button"
                          onClick={() => handlePreview(document)}
                        >
                          Preview
                        </button>

                        <button
                          type="button"
                          onClick={() => handleDownload(document)}
                        >
                          Download
                        </button>

                        <button
                          type="button"
                          onClick={() => handleRename(document)}
                        >
                          Rename
                        </button>

                        <button
                          type="button"
                          className="documents-delete-btn"
                          onClick={() => handleDelete(document)}
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          <div className="documents-pagination">
            <button
              type="button"
              disabled={page === 0}
              onClick={() =>
                setPage((current) => current - 1)
              }
            >
              Previous
            </button>

            <span>
              Page {page + 1} of {Math.max(totalPages, 1)}
            </span>

            <button
              type="button"
              disabled={page + 1 >= totalPages}
              onClick={() =>
                setPage((current) => current + 1)
              }
            >
              Next
            </button>
          </div>
        </>
      )}

      {/* Share Document Modal */}
      {shareDocument && (
        <div className="share-modal-overlay">
          <section
            className="share-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="share-modal-title"
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

            <h2 id="share-modal-title">Share Document</h2>

            <p>{shareDocument.fileName}</p>

            <form onSubmit={handleShare}>
              <label htmlFor="shareEmail">
                Recipient Email
              </label>

              <input
                id="shareEmail"
                type="email"
                value={shareEmail}
                onChange={(event) =>
                  setShareEmail(event.target.value)
                }
                placeholder="user@example.com"
                required
              />

              <label className="share-checkbox">
                <input
                  type="checkbox"
                  checked={canView}
                  onChange={(event) =>
                    setCanView(event.target.checked)
                  }
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
                  onChange={(event) =>
                    setCanShare(event.target.checked)
                  }
                />
                Allow Resharing
              </label>

              <label htmlFor="shareExpiry">
                Expiry Date and Time (Optional)
              </label>

              <input
                id="shareExpiry"
                type="datetime-local"
                value={expiresAt}
                onChange={(event) =>
                  setExpiresAt(event.target.value)
                }
              />

              <button
                type="submit"
                disabled={sharing}
              >
                {sharing ? "Sharing..." : "Share Document"}
              </button>
            </form>
          </section>
        </div>
      )}
    </main>
  );
}

export default Documents;

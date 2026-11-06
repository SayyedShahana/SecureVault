
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { documentApi } from "../services/api";
import "./UploadDocument.css";

function UploadDocument() {
  const navigate = useNavigate();

  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");

    if (!file) {
      setError("Please select a document to upload.");
      return;
    }

    try {
      setLoading(true);

      await documentApi.uploadDocument(file);

      navigate("/dashboard", {
        replace: true,
        state: { message: "Document uploaded successfully." },
      });
    } catch (err) {
      setError(err.message || "Document upload failed.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="upload-page">
      <section className="upload-card">
        <Link to="/dashboard" className="upload-back">
          ← Back to Dashboard
        </Link>

        <h1>Upload Document</h1>
        <p>Select a file to securely upload to your vault.</p>

        <form onSubmit={handleSubmit}>
          <label htmlFor="documentFile" className="upload-dropzone">
            <span className="upload-icon">↑</span>
            <strong>
              {file ? file.name : "Choose a document"}
            </strong>
            <span>
              {file
                ? `${(file.size / 1024 / 1024).toFixed(2)} MB`
                : "Click here to select a file"}
            </span>
          </label>

          <input
            id="documentFile"
            type="file"
            onChange={(event) => {
              setFile(event.target.files?.[0] || null);
              setError("");
            }}
          />

          <p className="upload-hint">Maximum file size: 10 MB</p>

          {error && (
            <p className="upload-error" role="alert">
              {error}
            </p>
          )}

          <button type="submit" disabled={loading || !file}>
            {loading ? "Uploading..." : "Upload Document"}
          </button>
        </form>
      </section>
    </main>
  );
}

export default UploadDocument;

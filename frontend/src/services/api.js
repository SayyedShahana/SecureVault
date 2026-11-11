
  const API_BASE_URL = "/api";

  let refreshPromise = null;

  function clearTokens() {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
  }

  async function performRefresh(rejectedAccessToken) {
    const currentAccessToken = localStorage.getItem("accessToken");

    // Another tab may already have refreshed the token.
    if (
      rejectedAccessToken &&
      currentAccessToken &&
      currentAccessToken !== rejectedAccessToken
    ) {
      return currentAccessToken;
    }

    const refreshToken = localStorage.getItem("refreshToken");

    if (!refreshToken) {
      clearTokens();
      throw new Error("Session expired. Please log in again.");
    }

    try {
      const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ refreshToken }),
      });

      const result = await response.json();

      if (
        !response.ok ||
        !result?.success ||
        !result?.data?.accessToken ||
        !result?.data?.refreshToken
      ) {
        throw new Error(
          result?.message || "Session expired. Please log in again."
        );
      }

      localStorage.setItem("accessToken", result.data.accessToken);
      localStorage.setItem("refreshToken", result.data.refreshToken);

      return result.data.accessToken;
    } catch (error) {
      clearTokens();
      throw new Error(error.message || "Session expired. Please log in again.");
    }
  }

  async function refreshSession(rejectedAccessToken) {
    // Coordinate refresh operations across browser tabs.
    if (navigator.locks?.request) {
      return navigator.locks.request(
        "securevault-token-refresh",
        () => performRefresh(rejectedAccessToken)
      );
    }

    // Fallback for browsers without Web Locks.
    if (refreshPromise) {
      return refreshPromise;
    }

    refreshPromise = performRefresh(rejectedAccessToken);

    try {
      return await refreshPromise;
    } finally {
      refreshPromise = null;
    }
  }

  async function request(endpoint, options = {}, isFileRequest = false) {
    for (let attempt = 0; attempt < 2; attempt++) {
      const accessToken = localStorage.getItem("accessToken");

      const headers = { ...options.headers };

      if (!endpoint.startsWith("/auth/") && accessToken) {
        headers.Authorization = `Bearer ${accessToken}`;
      } else {
        delete headers.Authorization;
      }

      if (!(options.body instanceof FormData) && !isFileRequest) {
        headers["Content-Type"] = "application/json";
      }

      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        headers,
      });

      if (
        response.status === 401 &&
        attempt === 0 &&
        !endpoint.startsWith("/auth/")
      ) {
        await refreshSession(accessToken);
        continue;
      }

      if (isFileRequest) {
        if (!response.ok) {
          let errorMessage = "Unable to retrieve the document.";

          try {
            const errorResult = await response.json();
            errorMessage = errorResult?.message || errorMessage;
          } catch {
            // Keep the default error message.
          }

          throw new Error(errorMessage);
        }

        return response;
      }

      let result;

      try {
        result = await response.json();
      } catch {
        result = null;
      }

      if (!response.ok || !result?.success) {
        throw new Error(result?.message || "Something went wrong.");
      }

      return result;
    }

    throw new Error("Request failed. Please try again.");
  }

  async function apiRequest(endpoint, options = {}) {
    return request(endpoint, options);
  }

  async function fileRequest(endpoint) {
    return request(endpoint, {}, true);
  }

  export const authApi = {
    register: (userData) =>
      apiRequest("/auth/register", {
        method: "POST",
        body: JSON.stringify(userData),
      }),

    login: (credentials) =>
      apiRequest("/auth/login", {
        method: "POST",
        body: JSON.stringify(credentials),
      }),

    refresh: (refreshToken) =>
      apiRequest("/auth/refresh", {
        method: "POST",
        body: JSON.stringify({ refreshToken }),
      }),

    logout: (refreshToken) =>
      apiRequest("/auth/logout", {
        method: "POST",
        body: JSON.stringify({ refreshToken }),
      }),
  };

  export const documentApi = {
    getDocuments: (params = {}) => {
      const query = new URLSearchParams();

      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== "") {
          query.append(key, value);
        }
      });

      const queryString = query.toString();

      return apiRequest(
        `/documents${queryString ? `?${queryString}` : ""}`
      );
    },

    uploadDocument: (file) => {
      const formData = new FormData();
      formData.append("file", file);

      return apiRequest("/documents", {
        method: "POST",
        body: formData,
      });
    },

    downloadDocument: (documentId) =>
      fileRequest(`/documents/${documentId}/download`),

    previewDocument: (documentId) =>
      fileRequest(`/documents/${documentId}/preview`),

    renameDocument: (documentId, newFileName) =>
      apiRequest(`/documents/${documentId}/rename`, {
        method: "PATCH",
        body: JSON.stringify({ newFileName }),
      }),

    deleteDocument: (documentId) =>
      apiRequest(`/documents/${documentId}`, {
        method: "DELETE",
      }),

    shareDocument: (documentId, shareData) =>
      apiRequest(`/documents/${documentId}/share`, {
        method: "POST",
        body: JSON.stringify(shareData),
      }),

    getSharedDocuments: () =>
      apiRequest("/documents/shared-with-me"),
    getSharedByMe: () =>
  apiRequest("/documents/shared-by-me"),

updateSharePermission: (documentId, shareId, permissionData) =>
  apiRequest(`/documents/${documentId}/shares/${shareId}`, {
    method: "PATCH",
    body: JSON.stringify(permissionData),
  }),

    revokeShare: (documentId, shareId) =>
      apiRequest(`/documents/${documentId}/shares/${shareId}`, {
        method: "DELETE",
      }),
  };

  export const adminApi = {
  getUsers: () =>
    apiRequest("/admin/users"),

  getAuditLogs: (page = 0, size = 10) =>
    apiRequest(`/admin/audit-logs?page=${page}&size=${size}`),
  getAuditLogSummary: () =>
  apiRequest("/admin/audit-logs/summary"),
};

export const auditLogApi = {
  getMyAuditLogs: (page = 0, size = 10) =>
    apiRequest(`/audit-logs/me?page=${page}&size=${size}`),
};
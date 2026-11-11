  import {
    createContext,
    useContext,
    useEffect,
    useState,
  } from "react";
  import { authApi } from "../services/api";

  const AuthContext = createContext(null);

  function decodeToken(token) {
    if (!token) return null;

    try {
      const payload = token.split(".")[1];

      if (!payload) return null;

      const base64 = payload
        .replace(/-/g, "+")
        .replace(/_/g, "/");

      const decodedPayload = atob(
        base64.padEnd(Math.ceil(base64.length / 4) * 4, "=")
      );

      return JSON.parse(decodedPayload);
    } catch {
      return null;
    }
  }

  function getUserFromToken(accessToken) {
    const payload = decodeToken(accessToken);

    if (!payload?.sub || !payload?.role) {
      return null;
    }

    return {
      email: payload.sub,
      role: payload.role.toUpperCase(),
    };
  }

  function getStoredTokens() {
    return {
      accessToken: localStorage.getItem("accessToken"),
      refreshToken: localStorage.getItem("refreshToken"),
    };
  }

  function clearTokens() {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
  }

  // Restore the session when the access token is missing.
  async function restoreStoredSession() {
    const restore = async () => {
      // Another tab may already have refreshed the session.
      const currentTokens = getStoredTokens();

      if (currentTokens.accessToken) {
        return currentTokens;
      }

      if (!currentTokens.refreshToken) {
        return {
          accessToken: null,
          refreshToken: null,
        };
      }

      const response = await authApi.refresh(
        currentTokens.refreshToken
      );

      const { accessToken, refreshToken } = response.data;

      if (!accessToken || !refreshToken) {
        throw new Error("Unable to restore your session.");
      }

      localStorage.setItem("accessToken", accessToken);
      localStorage.setItem("refreshToken", refreshToken);

      return {
        accessToken,
        refreshToken,
      };
    };

    // Use the same refresh lock as api.js.
    if (navigator.locks?.request) {
      return navigator.locks.request(
        "securevault-token-refresh",
        restore
      );
    }

    return restore();
  }

  export function AuthProvider({ children }) {
    const [tokens, setTokens] = useState(getStoredTokens);
    const [isInitializing, setIsInitializing] = useState(true);

    // Restore an existing session when the application starts.
    useEffect(() => {
      let active = true;

      const initializeSession = async () => {
        try {
          const restoredTokens = await restoreStoredSession();

          if (active) {
            setTokens(restoredTokens);
          }
        } catch {
          clearTokens();

          if (active) {
            setTokens({
              accessToken: null,
              refreshToken: null,
            });
          }
        } finally {
          if (active) {
            setIsInitializing(false);
          }
        }
      };

      initializeSession();

      return () => {
        active = false;
      };
    }, []);

    // Synchronize authentication changes across browser tabs.
    useEffect(() => {
      const synchronizeSession = async (event) => {
        if (
          event.key !== null &&
          event.key !== "accessToken" &&
          event.key !== "refreshToken"
        ) {
          return;
        }

        const currentTokens = getStoredTokens();

        if (currentTokens.accessToken) {
          setTokens(currentTokens);
          setIsInitializing(false);
          return;
        }

        if (currentTokens.refreshToken) {
          setIsInitializing(true);

          try {
            const restoredTokens = await restoreStoredSession();
            setTokens(restoredTokens);
          } catch {
            clearTokens();
            setTokens({
              accessToken: null,
              refreshToken: null,
            });
          } finally {
            setIsInitializing(false);
          }

          return;
        }

        setTokens({
          accessToken: null,
          refreshToken: null,
        });
        setIsInitializing(false);
      };

      window.addEventListener("storage", synchronizeSession);

      return () => {
        window.removeEventListener("storage", synchronizeSession);
      };
    }, []);

    const user = getUserFromToken(tokens.accessToken);
    const isAuthenticated = Boolean(tokens.accessToken && user);

    const login = async (credentials) => {
      if (isInitializing) {
        throw new Error("Please wait while your session is being checked.");
  }
const performLogin = async () => {
  const existingTokens = getStoredTokens();

  // Do not allow another login while a session exists.
  if (existingTokens.accessToken || existingTokens.refreshToken) {
    throw new Error(
      "Another account is already logged in. Please log out first."
    );
  }

  const response = await authApi.login(credentials);

  const { accessToken, refreshToken } = response.data;
  const loggedInUser = getUserFromToken(accessToken);

  if (!accessToken || !refreshToken || !loggedInUser) {
    throw new Error("Unable to identify your account.");
  }

  if (!["ADMIN", "USER"].includes(loggedInUser.role)) {
    throw new Error("Your account has an invalid role.");
  }

  localStorage.setItem("accessToken", accessToken);
  localStorage.setItem("refreshToken", refreshToken);

  setTokens({
    accessToken,
    refreshToken,
  });

  return {
    ...response,
    user: loggedInUser,
  };
};

      if (navigator.locks?.request) {
        return navigator.locks.request(
          "securevault-login",
          performLogin
        );
      }

      return performLogin();
    };

    const logout = async () => {
      const refreshToken = localStorage.getItem("refreshToken");

      try {
        if (refreshToken) {
          await authApi.logout(refreshToken);
        }
      } finally {
        clearTokens();

        setTokens({
          accessToken: null,
          refreshToken: null,
        });
      }
    };

    return (
      <AuthContext.Provider
        value={{
          tokens,
          user,
          role: user?.role || null,
          isAuthenticated,
          isInitializing,
          login,
          logout,
        }}
      >
        {children}
      </AuthContext.Provider>
    );
  }

  export function useAuth() {
    const context = useContext(AuthContext);

    if (!context) {
      throw new Error("useAuth must be used within an AuthProvider");
    }

    return context;
  }

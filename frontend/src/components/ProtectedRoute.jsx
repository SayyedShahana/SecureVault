import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

function ProtectedRoute({ allowedRoles }) {
  const {
    isAuthenticated,
    isInitializing,
    role,
  } = useAuth();

  // Wait until the refresh-token restoration finishes.
  if (isInitializing) {
    return (
      <div
        style={{
          minHeight: "100vh",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          color: "#173b68",
          fontSize: "16px",
        }}
      >
        Restoring your session...
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(role)) {
    const redirectPath =
      role === "ADMIN" ? "/admin" : "/dashboard";

    return <Navigate to={redirectPath} replace />;
  }

  return <Outlet />;
}

export default ProtectedRoute;
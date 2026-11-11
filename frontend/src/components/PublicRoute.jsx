
import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

function PublicRoute() {
  const {
    isAuthenticated,
    isInitializing,
    role,
  } = useAuth();

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

  if (isAuthenticated) {
    return (
      <Navigate
        to={role === "ADMIN" ? "/admin" : "/dashboard"}
        replace
      />
    );
  }

  return <Outlet />;
}

export default PublicRoute;

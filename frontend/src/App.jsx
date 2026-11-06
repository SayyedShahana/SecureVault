import { Navigate, Route, Routes } from "react-router-dom";

import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import UploadDocument from "./pages/UploadDocument";
import Documents from "./pages/Documents";
import AdminDashboard from "./pages/AdminDashboard";
import ProtectedRoute from "./components/ProtectedRoute";
import AuditLogs from "./pages/AuditLogs";
import SharedByMe from "./pages/SharedByMe";
function App() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      {/* USER routes */}
      <Route element={<ProtectedRoute allowedRoles={["USER"]} />}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/documents" element={<Documents />} />
        <Route path="/documents/upload" element={<UploadDocument />} />
        <Route path="/shared-by-me" element={<SharedByMe />} />
      </Route>

      {/* ADMIN routes */}
      <Route element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
        <Route path="/admin" element={<AdminDashboard />} />
      </Route>

      {/* USER and ADMIN routes */}
      <Route element={<ProtectedRoute allowedRoles={["USER", "ADMIN"]} />}>
        <Route path="/audit-logs" element={<AuditLogs />} />
      </Route>

      {/* Fallback route */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

export default App;
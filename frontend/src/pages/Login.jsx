
import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import AuthLayout from "../components/AuthLayout";
import FormField from "../components/FormField";
import PasswordField from "../components/PasswordField";
import { useAuth } from "../context/AuthContext";
import "../styles/AuthForms.css";

function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(
    location.state?.message || ""
  );
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setSuccess("");

    const trimmedEmail = email.trim();

    const emailRegex =
      /^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$/;

    if (!trimmedEmail) {
      setError("Please enter your email address.");
      return;
    }

    if (!emailRegex.test(trimmedEmail)) {
      setError("Please enter a valid email address.");
      return;
    }

    if (!password) {
      setError("Please enter your password.");
      return;
    }

    if (password.length > 72) {
      setError("Password cannot exceed 72 characters.");
      return;
    }

    setLoading(true);

    try {
      const response = await login({
        email: trimmedEmail,
        password,
      });
      console.log("LOGIN RESPONSE:", response);
console.log("USER ROLE:", response?.user?.role);

      const role = response.user.role;

      if (role === "ADMIN") {
        navigate("/admin", {
          replace: true,
          state: {
            message: "Welcome to SecureVault Admin Panel.",
          },
        });
      } else {
        navigate("/dashboard", {
          replace: true,
          state: {
            message: "Login successful! Welcome to SecureVault.",
          },
        });
      }
    } catch (err) {
      setError(err.message || "Unable to log in. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Welcome back"
      subtitle="Sign in to access your secure documents."
      footerText="Don't have an account?"
      footerLink="/register"
      footerLabel="Create account"
    >
      {success && (
        <p className="form-success" role="status">
          {success}
        </p>
      )}

      <form onSubmit={handleSubmit}>
        <FormField
          id="email"
          label="Email address"
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          placeholder="Enter your email"
          autoComplete="email"
        />

        <PasswordField
          id="password"
          label="Password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          autoComplete="current-password"
        />

        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}

        <button
          className="auth-submit-button"
          type="submit"
          disabled={loading}
        >
          {loading ? "Signing in..." : "Sign in"}
        </button>
      </form>

      <p className="auth-switch-link">
        New to SecureVault? <Link to="/register">Register here</Link>
      </p>
    </AuthLayout>
  );
}

export default Login;

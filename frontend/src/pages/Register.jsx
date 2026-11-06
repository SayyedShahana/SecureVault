
import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import AuthLayout from "../components/AuthLayout";
import FormField from "../components/FormField";
import PasswordField from "../components/PasswordField";
import { authApi } from "../services/api";
import "../styles/AuthForms.css";

function Register() {
  const navigate = useNavigate();

  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");

    const trimmedName = fullName.trim();
    const trimmedEmail = email.trim();

    // Full name validation
    const nameRegex = /^[\p{L}]+(?: [\p{L}]+)*$/u;

    if (!trimmedName) {
      setError("Please enter your full name.");
      return;
    }

    if (trimmedName.length > 100) {
      setError("Full name cannot exceed 100 characters.");
      return;
    }

    if (!nameRegex.test(trimmedName)) {
      setError("Full name must contain only alphabets and single spaces.");
      return;
    }

    // Email validation
    const emailRegex =
      /^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$/;

    if (!trimmedEmail) {
      setError("Please enter your email address.");
      return;
    }

    if (!emailRegex.test(trimmedEmail)) {
      setError("Please enter a valid email address with a domain extension.");
      return;
    }

    // Password length validation
    if (!password) {
      setError("Please enter your password.");
      return;
    }

    if (password.length < 8 || password.length > 72) {
      setError("Password must contain between 8 and 72 characters.");
      return;
    }

    // Password complexity validation
    const passwordRegex =
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).+$/;

    if (!passwordRegex.test(password)) {
      setError(
        "Password must contain uppercase, lowercase, number and special character."
      );
      return;
    }

    setLoading(true);

    try {
      await authApi.register({
        fullName: trimmedName,
        email: trimmedEmail,
        password,
      });

      navigate("/login", {
        replace: true,
        state: { message: "Registration successful. Please log in." },
      });
    } catch (err) {
      setError(err.message || "Registration failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Create your account"
      subtitle="Register to start managing your documents securely."
      footerText="Already have an account?"
      footerLink="/login"
      footerLabel="Sign in"
    >
      <form onSubmit={handleSubmit}>
        <FormField
          id="fullName"
          label="Full name"
          value={fullName}
          onChange={(event) => setFullName(event.target.value)}
          placeholder="Enter your full name"
          autoComplete="name"
        />

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
          autoComplete="new-password"
        />

        
<div className="password-requirements">
  <p className="password-requirements-title">
    Password Requirements
  </p>

  <div className="password-requirements-list">
    {[
      {
        label: "At least 8 characters",
        valid: password.length >= 8,
      },
      {
        label: "One uppercase letter (A-Z)",
        valid: /[A-Z]/.test(password),
      },
      {
        label: "One lowercase letter (a-z)",
        valid: /[a-z]/.test(password),
      },
      {
        label: "One number (0-9)",
        valid: /\d/.test(password),
      },
      {
        label: "One special character (@#$)",
        valid: /[^A-Za-z0-9\s]/.test(password),
      },
    ].map((requirement) => (
      <div
        key={requirement.label}
        className={`password-requirement ${
          requirement.valid ? "valid" : ""
        }`}
      >
        <span className="requirement-icon">
          {requirement.valid ? "✓" : ""}
        </span>
        <span>{requirement.label}</span>
      </div>
    ))}
  </div>
</div>


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
          {loading ? "Creating account..." : "Create account"}
        </button>
      </form>
    </AuthLayout>
  );
}

export default Register;

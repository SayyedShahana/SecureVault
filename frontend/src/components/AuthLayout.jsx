import { Link } from "react-router-dom";
import "./AuthLayout.css";

function AuthLayout({
  title,
  subtitle,
  children,
  footerText,
  footerLink,
  footerLabel,
}) {
  return (
    <main className="auth-page">
      <section className="auth-card">
        <Link to="/login" className="auth-brand">
          <span className="auth-brand-icon">S</span>
          <span>SecureVault</span>
        </Link>

        <div className="auth-heading">
          <h1>{title}</h1>
          <p>{subtitle}</p>
        </div>

        {children}

        <p className="auth-footer">
          {footerText}{" "}
          <Link to={footerLink}>{footerLabel}</Link>
        </p>
      </section>

      <p className="auth-security-note">
        Your documents. Your privacy. Your control.
      </p>
    </main>
  );
}

export default AuthLayout;
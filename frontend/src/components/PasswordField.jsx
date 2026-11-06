
import { useState } from "react";
import "./PasswordField.css";

function PasswordField({
  id = "password",
  label = "Password",
  value,
  onChange,
  autoComplete = "current-password",
}) {
  const [visible, setVisible] = useState(false);

  return (
    <div className="form-field">
      <label htmlFor={id}>{label}</label>

      <div className="password-input-wrapper">
        <input
          id={id}
          name={id}
          type={visible ? "text" : "password"}
          value={value}
          onChange={onChange}
          autoComplete={autoComplete}
          placeholder="Enter your password"
          maxLength={72}
          required
        />

        <button
          type="button"
          className="password-toggle"
          onClick={() => setVisible((current) => !current)}
          aria-label={visible ? "Hide password" : "Show password"}
        >
          {visible ? "Hide" : "Show"}
        </button>
      </div>
    </div>
  );
}

export default PasswordField;

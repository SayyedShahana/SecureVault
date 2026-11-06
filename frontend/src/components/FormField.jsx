import "./FormField.css";

function FormField({
  id,
  label,
  type = "text",
  value,
  onChange,
  placeholder,
  autoComplete,
  required = true,
}) {
  return (
    <div className="form-field">
      <label htmlFor={id}>{label}</label>

      <input
        id={id}
        name={id}
        type={type}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        autoComplete={autoComplete}
        required={required}
      />
    </div>
  );
}

export default FormField;
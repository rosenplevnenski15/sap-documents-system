import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { validatePassword, validateUsername } from "../lib/validation";

export default function RegisterPage() {
  const { register } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    try {
      const usernameError = validateUsername(username);
      const passwordError = validatePassword(password);
      if (usernameError || passwordError) {
        throw new Error(usernameError ?? passwordError ?? "Invalid registration data.");
      }
      setError("");
      await register(username, password);
      setMessage("User registered successfully. You can now sign in.");
      showToast("Registration successful.", "success");
      setTimeout(() => navigate("/login"), 800);
    } catch (e) {
      const value = e instanceof Error ? e.message : "Registration failed";
      setError(value);
      showToast(value, "error");
    }
  }

  return (
    <div className="auth-card">
      <h1>Create account</h1>
      <form onSubmit={handleSubmit}>
        <label>Username</label>
        <input value={username} onChange={(e) => setUsername(e.target.value)} />
        <label>Password</label>
        <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        {message && <p className="ok">{message}</p>}
        {error && <p className="error">{error}</p>}
        <button type="submit">Register</button>
      </form>
      <p>
        Already have an account? <Link to="/login">Sign in</Link>
      </p>
    </div>
  );
}

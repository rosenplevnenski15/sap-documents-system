import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { validatePassword, validateUsername } from "../lib/validation";

export default function LoginPage() {
  const { login } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    try {
      const usernameError = validateUsername(username);
      const passwordError = validatePassword(password);
      if (usernameError || passwordError) {
        throw new Error(usernameError ?? passwordError ?? "Invalid credentials.");
      }
      setError("");
      await login(username, password);
      showToast("Signed in successfully.", "success");
      navigate("/");
    } catch (e) {
      const message = e instanceof Error ? e.message : "Login failed";
      setError(message);
      showToast(message, "error");
    }
  }

  return (
    <div className="auth-card">
      <h1>Sign in</h1>
      <form onSubmit={handleSubmit}>
        <label>Username</label>
        <input value={username} onChange={(e) => setUsername(e.target.value)} />
        <label>Password</label>
        <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        {error && <p className="error">{error}</p>}
        <button type="submit">Login</button>
      </form>
      <p>
        No account? <Link to="/register">Register</Link>
      </p>
    </div>
  );
}

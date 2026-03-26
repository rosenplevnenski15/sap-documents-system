import { Link, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function Layout() {
  const { session, logout } = useAuth();

  return (
    <div className="app">
      <aside className="sidebar">
        <h2>Document Portal</h2>
        <p className="muted">{session?.user.username}</p>
        <p className="muted">{session?.user.role}</p>
        <nav>
          <Link to="/">Operations</Link>
          <Link to="/documents">Document Registry</Link>
          {session?.user.role === "ADMIN" && <Link to="/admin/users">User Management</Link>}
        </nav>
        <button onClick={logout}>Sign out</button>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}

import { useEffect, useState } from "react";
import { api } from "../lib/api";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import type { Role, User } from "../types";

const roles: Role[] = ["ADMIN", "AUTHOR", "REVIEWER", "READER"];

export default function AdminUsersPage() {
  const { session } = useAuth();
  const { showToast } = useToast();
  const token = session!.accessToken;
  const [users, setUsers] = useState<User[]>([]);
  const [error, setError] = useState("");

  async function load() {
    try {
      setError("");
      setUsers(await api.getUsers(token));
    } catch (e) {
      const message = e instanceof Error ? e.message : "Failed to load users";
      setError(message);
      showToast(message, "error");
    }
  }

  useEffect(() => {
    void load();
  }, []);

  return (
    <section>
      <h1>User Management</h1>
      {error && <p className="error">{error}</p>}
      <div className="panel">
        <table>
          <thead>
            <tr>
              <th>Username</th>
              <th>Role</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>{user.username}</td>
                <td>{user.role}</td>
                <td className="row-actions">
                  {roles.map((role) => (
                    <button
                      key={role}
                      onClick={() =>
                        api
                          .updateRole(user.id, role, token)
                          .then(() => showToast(`Role updated to ${role}.`, "success"))
                          .then(load)
                      }
                    >
                      {role}
                    </button>
                  ))}
                  <button
                    onClick={() =>
                      api
                        .deactivateUser(user.id, token)
                        .then(() => showToast("User deactivated.", "success"))
                        .then(load)
                    }
                  >
                    Deactivate
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

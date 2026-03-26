import { createContext, useContext, useMemo, useState } from "react";
import { api, configureApiAuth } from "../lib/api";
import { clearSession, loadSession, saveSession } from "../lib/auth";
import type { LoginResponse } from "../types";

interface AuthContextType {
  session: LoginResponse | null;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<LoginResponse | null>(() => loadSession());

  configureApiAuth(
    () => session,
    (next) => {
      setSession(next);
      if (next) saveSession(next);
      else clearSession();
    }
  );

  const value = useMemo<AuthContextType>(
    () => ({
      session,
      async login(username, password) {
        const payload = await api.login(username, password);
        setSession(payload);
        saveSession(payload);
      },
      async register(username, password) {
        await api.register(username, password);
      },
      logout() {
        setSession(null);
        clearSession();
      }
    }),
    [session]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}

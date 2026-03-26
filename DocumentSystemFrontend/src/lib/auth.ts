import type { LoginResponse } from "../types";

const STORAGE_KEY = "docsys.session";

export function saveSession(payload: LoginResponse): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
}

export function loadSession(): LoginResponse | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as LoginResponse;
  } catch {
    return null;
  }
}

export function clearSession(): void {
  localStorage.removeItem(STORAGE_KEY);
}

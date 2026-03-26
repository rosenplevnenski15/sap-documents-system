import type { CommentResponse, DocumentResponse, LoginResponse, PageResponse, Role, User, VersionResponse } from "../types";

const API_BASE_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

type Method = "GET" | "POST" | "PUT";
type ResponseType = "json" | "text" | "blob";
type SessionGetter = () => LoginResponse | null;
type SessionSetter = (value: LoginResponse | null) => void;

let getSession: SessionGetter | null = null;
let setSession: SessionSetter | null = null;
let inFlightRefresh: Promise<LoginResponse> | null = null;

function cloneWithAccessToken(session: LoginResponse, accessToken: string, expiresIn: number): LoginResponse {
  return { ...session, accessToken, expiresIn };
}

export function configureApiAuth(sessionGetter: SessionGetter, sessionSetter: SessionSetter): void {
  getSession = sessionGetter;
  setSession = sessionSetter;
}

async function refreshToken(): Promise<LoginResponse> {
  if (inFlightRefresh) return inFlightRefresh;
  const session = getSession?.();
  if (!session?.refreshToken) throw new Error("No refresh token available");
  inFlightRefresh = fetch(`${API_BASE_URL}/api/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken: session.refreshToken })
  })
    .then(async (res) => {
      if (!res.ok) throw new Error("Session expired. Please login again.");
      const payload = (await res.json()) as LoginResponse;
      const next = cloneWithAccessToken(session, payload.accessToken, payload.expiresIn);
      setSession?.(next);
      return next;
    })
    .finally(() => {
      inFlightRefresh = null;
    });
  return inFlightRefresh;
}

async function request<T>(
  path: string,
  method: Method = "GET",
  token?: string,
  body?: BodyInit | null,
  isJson = true,
  responseType: ResponseType = "json",
  hasRetried = false
): Promise<T> {
  const headers: Record<string, string> = {};
  const activeToken = token ?? getSession?.()?.accessToken;
  if (activeToken) headers.Authorization = `Bearer ${activeToken}`;
  if (isJson) headers["Content-Type"] = "application/json";

  const res = await fetch(`${API_BASE_URL}${path}`, { method, headers, body });
  if (res.status === 401 && !hasRetried && getSession) {
    const refreshed = await refreshToken();
    return request<T>(path, method, refreshed.accessToken, body, isJson, responseType, true);
  }
  if (!res.ok) {
    const txt = await res.text();
    throw new Error(txt || `Request failed with ${res.status}`);
  }
  if (res.status === 204) return null as T;
  if (responseType === "text") return (await res.text()) as T;
  if (responseType === "blob") return (await res.blob()) as T;
  return (await res.json()) as T;
}

export const api = {
  login: (username: string, password: string) =>
    request<LoginResponse>("/api/auth/login", "POST", undefined, JSON.stringify({ username, password })),
  register: (username: string, password: string) =>
    request<{ message: string }>("/api/auth/register", "POST", undefined, JSON.stringify({ username, password })),
  createDocument: (title: string, file: File, token: string) => {
    const form = new FormData();
    form.append("title", title);
    form.append("file", file);
    return request<DocumentResponse>("/api/documents", "POST", token, form, false);
  },
  getVersions: (documentId: string, token: string) =>
    request<VersionResponse[]>(`/api/versions/${documentId}/versions`, "GET", token),
  getActiveVersion: (documentId: string, token: string) =>
    request<VersionResponse>(`/api/versions/${documentId}/active`, "GET", token),
  createVersion: (documentId: string, file: File, token: string) => {
    const form = new FormData();
    form.append("file", file);
    return request<VersionResponse>(`/api/versions/documents/${documentId}/versions`, "POST", token, form, false);
  },
  submitVersion: (versionId: string, token: string) =>
    request<VersionResponse>(`/api/versions/${versionId}/submit`, "POST", token, ""),
  approveVersion: (versionId: string, token: string) =>
    request<VersionResponse>(`/api/versions/${versionId}/approve`, "POST", token, ""),
  rejectVersion: (versionId: string, token: string) =>
    request<VersionResponse>(`/api/versions/${versionId}/reject`, "POST", token, ""),
  compareLatest: (documentId: string, token: string) =>
    request<string>(`/api/documents/${documentId}/compare`, "GET", token, undefined, true, "text"),
  listDocuments: (
    token: string,
    query = "",
    mine = false,
    page = 0,
    size = 10,
    sortBy = "createdAt",
    direction: "asc" | "desc" = "desc"
  ) =>
    request<PageResponse<DocumentResponse>>(
      `/api/documents?query=${encodeURIComponent(query)}&mine=${mine}&page=${page}&size=${size}&sortBy=${sortBy}&direction=${direction}`,
      "GET",
      token
    ),
  exportPdf: (versionId: string, token: string) =>
    request<Blob>(`/api/versions/${versionId}/export/pdf`, "GET", token, undefined, true, "blob"),
  getComments: (versionId: string, token: string) =>
    request<CommentResponse[]>(`/api/versions/${versionId}/comments`, "GET", token),
  addComment: (versionId: string, content: string, token: string) =>
    request<CommentResponse>(`/api/versions/${versionId}/comments`, "POST", token, JSON.stringify({ content })),
  getUsers: (token: string) => request<User[]>("/api/users", "GET", token),
  updateRole: (userId: string, role: Role, token: string) =>
    request<{ message: string }>(`/api/users/${userId}/role?role=${role}`, "PUT", token, ""),
  deactivateUser: (userId: string, token: string) =>
    request<{ message: string }>(`/api/users/${userId}/deactivate`, "PUT", token, "")
};

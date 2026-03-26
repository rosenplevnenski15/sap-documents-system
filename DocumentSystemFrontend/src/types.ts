export type Role = "ADMIN" | "AUTHOR" | "REVIEWER" | "READER";

export interface User {
  id: string;
  username: string;
  role: Role;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: User;
}

export interface DocumentResponse {
  id: string;
  title: string;
  createdBy: User;
  createdAt: string;
}

export interface DocumentDto {
  id: string;
  title: string;
}

export interface VersionResponse {
  id: string;
  versionNumber: number;
  fileName: string;
  status: string;
  isActive: boolean;
  createdAt: string;
  approvedAt: string | null;
  createdBy: User;
  approvedBy: User | null;
  document: DocumentDto;
}

export interface CommentResponse {
  id: string;
  content: string;
  createdAt: string;
  createdBy: User;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

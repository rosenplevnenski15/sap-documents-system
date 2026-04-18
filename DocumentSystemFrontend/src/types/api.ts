export type Role = 'ADMIN' | 'AUTHOR' | 'REVIEWER' | 'READER';

export type VersionStatus = 'DRAFT' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED';

export interface UserDto {
  id: string;
  username: string;
  role: Role;
  isActive?: boolean | string;
  active?: boolean | string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: UserDto;
}

export interface ApiResponse {
  message: string;
}

export interface DocumentDto {
  id: string;
  title: string;
}

export interface DocumentResponse {
  id: string;
  title: string;
  createdBy: UserDto;
  createdAt: string;
}

export interface VersionResponse {
  id: string;
  versionNumber: number;
  fileName: string;
  status: VersionStatus;
  isActive: boolean;
  createdAt: string;
  approvedAt?: string;
  content?: string;
  createdBy: UserDto;
  approvedBy?: UserDto;
  document: DocumentDto;
}

export interface VersionContentResponse {
  id: string;
  versionNumber: number;
  fileName: string;
  status: string;
  isActive: boolean;
  content: string;
}

export interface CreateCommentRequest {
  content: string;
}

export interface CommentResponse {
  id: string;
  content: string;
  createdAt: string;
  user: UserDto;
}

export interface CompareResponse {
  fileName1: string;
  fileName2: string;
  version1Content: string;
  version2Content: string;
  version1Number: number;
  version2Number: number;
}

export interface ErrorResponse {
  timestamp?: string;
  status: number;
  error: string;
  message: string;
  path?: string;
  validationErrors?: Record<string, string>;
}

import { apiClient } from './client';
import type {
  ApiResponse,
  LoginRequest,
  LoginResponse,
  RefreshTokenRequest,
  RegisterRequest,
} from '../types/api';

type RawLoginResponse = LoginResponse & {
  token?: string;
};

function normalizeLoginResponse(payload: RawLoginResponse): LoginResponse {
  const accessToken = payload.accessToken ?? payload.token;

  return {
    accessToken,
    refreshToken: payload.refreshToken,
    expiresIn: payload.expiresIn,
    user: payload.user,
  };
}

export const authApi = {
  async login(payload: LoginRequest) {
    const response = await apiClient.post<RawLoginResponse>('/auth/login', payload);
    return normalizeLoginResponse(response.data);
  },
  async register(payload: RegisterRequest) {
    const response = await apiClient.post<ApiResponse>('/auth/register', payload);
    return response.data;
  },
  async refreshToken(payload: RefreshTokenRequest) {
    const response = await apiClient.post<RawLoginResponse>('/auth/refresh', payload);
    return normalizeLoginResponse(response.data);
  },
  async logout() {
    const response = await apiClient.post<ApiResponse>('/auth/logout');
    return response.data;
  },
};

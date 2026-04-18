import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '../store/auth.store';
import type { LoginResponse } from '../types/api';

type RawLoginResponse = LoginResponse & {
  token?: string;
};

const baseURL =
  import.meta.env.VITE_API_BASE_URL?.toString() || 'http://localhost:8080/api';

export const apiClient = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
});

let isRefreshing = false;
let refreshSubscribers: Array<(token: string) => void> = [];

const PUBLIC_AUTH_PATHS = ['/auth/login', '/auth/register'];

function getRequestPath(config: InternalAxiosRequestConfig) {
  const url = config.url ?? '';
  if (url.startsWith('http://') || url.startsWith('https://')) {
    try {
      return new URL(url).pathname;
    } catch {
      return url;
    }
  }

  const normalized = url.startsWith('/') ? url : `/${url}`;
  return normalized;
}

function isPublicRequest(config: InternalAxiosRequestConfig) {
  const path = getRequestPath(config);
  return PUBLIC_AUTH_PATHS.some((publicPath) => path.endsWith(publicPath));
}

function subscribeTokenRefresh(callback: (token: string) => void) {
  refreshSubscribers.push(callback);
}

function onRefreshed(token: string) {
  refreshSubscribers.forEach((callback) => callback(token));
  refreshSubscribers = [];
}

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (isPublicRequest(config)) {
    if (config.headers?.Authorization) {
      delete config.headers.Authorization;
    }
    return config;
  }

  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };

    if (originalRequest && isPublicRequest(originalRequest)) {
      return Promise.reject(error);
    }

    if (error.response?.status !== 401 || !originalRequest || originalRequest._retry) {
      return Promise.reject(error);
    }

    const { refreshToken, clearSession, setSession } = useAuthStore.getState();
    if (!refreshToken) {
      clearSession();
      return Promise.reject(error);
    }

    if (isRefreshing) {
      return new Promise((resolve) => {
        subscribeTokenRefresh((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          resolve(apiClient(originalRequest));
        });
      });
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      const refreshResponse = await axios.post<RawLoginResponse>(`${baseURL}/auth/refresh`, {
        refreshToken,
      });

      const payload = refreshResponse.data;
      const nextAccessToken = payload.accessToken ?? payload.token;
      if (!nextAccessToken) {
        throw new Error('Refresh response does not include an access token');
      }
      setSession({
        accessToken: nextAccessToken,
        refreshToken: payload.refreshToken,
        expiresIn: payload.expiresIn,
        user: payload.user,
      });
      onRefreshed(nextAccessToken);
      originalRequest.headers.Authorization = `Bearer ${nextAccessToken}`;

      return apiClient(originalRequest);
    } catch (refreshError) {
      clearSession();
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  },
);

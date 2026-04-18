import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Role, UserDto } from '../types/api';

const ACCESS_TOKEN_STORAGE_KEY = 'document-system-access-token';

function getStoredAccessToken() {
  if (typeof window === 'undefined') {
    return null;
  }
  return window.localStorage.getItem(ACCESS_TOKEN_STORAGE_KEY);
}

function persistAccessToken(token: string | null) {
  if (typeof window === 'undefined') {
    return;
  }

  if (token) {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, token);
    return;
  }

  window.localStorage.removeItem(ACCESS_TOKEN_STORAGE_KEY);
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  expiresIn: number | null;
  user: UserDto | null;
  isAuthenticated: boolean;
  setSession: (payload: {
    accessToken: string;
    refreshToken: string;
    expiresIn: number;
    user: UserDto;
  }) => void;
  clearSession: () => void;
  hasRole: (roles: Role[]) => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: getStoredAccessToken(),
      refreshToken: null,
      expiresIn: null,
      user: null,
      isAuthenticated: !!getStoredAccessToken(),
      setSession: ({ accessToken, refreshToken, expiresIn, user }) => {
        persistAccessToken(accessToken);
        set({
          accessToken,
          refreshToken,
          expiresIn,
          user,
          isAuthenticated: true,
        });
      },
      clearSession: () => {
        persistAccessToken(null);
        set({
          accessToken: null,
          refreshToken: null,
          expiresIn: null,
          user: null,
          isAuthenticated: false,
        });
      },
      hasRole: (roles) => {
        const currentRole = get().user?.role;
        return !!currentRole && roles.includes(currentRole);
      },
    }),
    {
      name: 'document-system-auth',
    },
  ),
);

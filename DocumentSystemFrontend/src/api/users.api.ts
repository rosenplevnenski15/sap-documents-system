import { apiClient } from './client';
import type { ApiResponse, Role, UserDto } from '../types/api';

type RawUser = UserDto & {
  active?: boolean | string | number | null;
  is_active?: boolean | string | number | null;
};

function toBooleanStatus(value: unknown) {
  if (typeof value === 'boolean') {
    return value;
  }

  if (typeof value === 'number') {
    return value === 1;
  }

  if (typeof value === 'string') {
    const normalized = value.trim().toLowerCase();
    return normalized === 'true' || normalized === '1' || normalized === 'yes' || normalized === 'active';
  }

  return false;
}

function normalizeUsersPayload(payload: RawUser[] | { data?: RawUser[] }) {
  const users = Array.isArray(payload) ? payload : payload.data ?? [];

  return users.map((user) => ({
    ...user,
    isActive: toBooleanStatus(user.isActive ?? user.active ?? user.is_active),
  }));
}

export const usersApi = {
  async getAllUsers() {
    const response = await apiClient.get<RawUser[] | { data?: RawUser[] }>('/users');
    return normalizeUsersPayload(response.data);
  },

  async changeRole(userId: string, role: Role) {
    const response = await apiClient.put<ApiResponse>(`/users/${userId}/role`, null, {
      params: { role },
    });
    return response.data;
  },

  async deactivateUser(userId: string) {
    const response = await apiClient.put<ApiResponse>(`/users/${userId}/deactivate`);
    return response.data;
  },

  async activateUser(userId: string) {
    const response = await apiClient.put<ApiResponse>(`/users/${userId}/activate`);
    return response.data;
  },
};

import { apiClient } from './client';
import type { AuthResponse } from '../types';

export const authApi = {
  login: (username: string, password: string) =>
    apiClient.post<AuthResponse>('/auth/login', { username, password }).then((r) => r.data),

  signup: (params: { username: string; email: string; password: string; phone: string }) =>
    apiClient.post<AuthResponse>('/auth/signup', params).then((r) => r.data),

  changePassword: (currentPassword: string, newPassword: string) =>
    apiClient.patch('/users/me/password', { currentPassword, newPassword }).then((r) => r.data),
};

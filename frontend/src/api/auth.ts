import { apiClient } from './client';
import type { AuthResponse } from '../types';

export const authApi = {
  login: (username: string, password: string) =>
    apiClient.post<AuthResponse>('/auth/login', { username, password }).then((r) => r.data),

  signup: (params: { username: string; email: string; password: string; phone: string }) =>
    apiClient.post<AuthResponse>('/auth/signup', params).then((r) => r.data),

  changePassword: (currentPassword: string, newPassword: string) =>
    apiClient.patch('/users/me/password', { currentPassword, newPassword }).then((r) => r.data),

  // Backs the signup form's live availability indicator - GET, unauthenticated
  // (matches /api/auth/** permitAll on the backend), safe to call before the
  // user has any token.
  checkUsername: (username: string) =>
    apiClient
      .get<{ username: string; available: boolean }>('/auth/check-username', { params: { username } })
      .then((r) => r.data),

  // Always resolves successfully regardless of whether the email is
  // registered - the backend deliberately never reveals account existence.
  forgotPassword: (email: string) => apiClient.post<void>('/auth/forgot-password', { email }).then((r) => r.data),

  resetPassword: (token: string, newPassword: string) =>
    apiClient.post<void>('/auth/reset-password', { token, newPassword }).then((r) => r.data),
};

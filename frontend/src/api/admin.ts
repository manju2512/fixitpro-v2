import { apiClient } from './client';
import type { DashboardStats, Role, Technician, UserSummary } from '../types';

export const adminApi = {
  listUsers: (role?: Role) =>
    apiClient.get<UserSummary[]>('/admin/users', { params: role ? { role } : undefined }).then((r) => r.data),

  setUserActive: (userId: number, active: boolean) =>
    apiClient.patch(`/admin/users/${userId}/status`, null, { params: { active } }).then((r) => r.data),

  createTechnician: (params: {
    username: string;
    email: string;
    password: string;
    phone: string;
    serviceTypeId: number;
    name: string;
    bio: string;
    yearsExperience: number;
  }) => apiClient.post<Technician>('/admin/technicians', params).then((r) => r.data),

  listAllTechnicians: () => apiClient.get<Technician[]>('/admin/technicians').then((r) => r.data),

  updateTechnician: (id: number, params: Partial<Pick<Technician, 'bio' | 'yearsExperience' | 'available'>>) =>
    apiClient.put<Technician>(`/admin/technicians/${id}`, params).then((r) => r.data),

  dashboardStats: () => apiClient.get<DashboardStats>('/admin/dashboard/stats').then((r) => r.data),
};

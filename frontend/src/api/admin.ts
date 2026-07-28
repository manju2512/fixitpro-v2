import { apiClient } from './client';
import type { DashboardStats, Role, ServiceType, Technician, UserSummary } from '../types';

export const adminApi = {
  listUsers: (role?: Role) =>
    apiClient.get<UserSummary[]>('/admin/users', { params: role ? { role } : undefined }).then((r) => r.data),

  setUserActive: (userId: number, active: boolean) =>
    apiClient.patch(`/admin/users/${userId}/status`, null, { params: { active } }).then((r) => r.data),

  // Matches AdminCreateTechnicianRequest exactly. There's no separate "name"
  // field - the technician's display name is just their username
  // (TechnicianResponse.from() reads it off the User entity).
  createTechnician: (params: {
    username: string;
    email: string;
    password: string;
    phone: string;
    serviceTypeId: number;
    bio?: string;
    yearsExperience?: number;
  }) => apiClient.post<Technician>('/admin/technicians', params).then((r) => r.data),

  listAllTechnicians: () => apiClient.get<Technician[]>('/admin/technicians').then((r) => r.data),

  // Matches UpdateTechnicianRequest exactly. serviceTypeId is required by
  // the backend (it re-resolves the ServiceType entity) even when you're
  // only changing bio/experience - it does NOT touch availability.
  updateTechnician: (id: number, params: { serviceTypeId: number; bio?: string; yearsExperience?: number }) =>
    apiClient.put<Technician>(`/admin/technicians/${id}`, params).then((r) => r.data),

  // Separate endpoint - availability isn't part of the update DTO above.
  setTechnicianAvailability: (id: number, available: boolean) =>
    apiClient
      .patch<void>(`/admin/technicians/${id}/availability`, null, { params: { available } })
      .then((r) => r.data),

  dashboardStats: () => apiClient.get<DashboardStats>('/admin/dashboard/stats').then((r) => r.data),

  // Service types - the trades FixitPro offers (Electrician, Plumber,
  // Carpenter, and whatever gets added later - Housekeeping, Painting, etc).
  // listAllServiceTypes includes inactive ones (the public /service-types
  // endpoint only returns active) so a deactivated one can be found and
  // turned back on.
  listAllServiceTypes: () => apiClient.get<ServiceType[]>('/admin/service-types').then((r) => r.data),

  createServiceType: (params: { name: string; description: string; basePrice: number }) =>
    apiClient.post<ServiceType>('/admin/service-types', params).then((r) => r.data),

  updateServiceType: (id: number, params: { name: string; description: string; basePrice: number }) =>
    apiClient.put<ServiceType>(`/admin/service-types/${id}`, params).then((r) => r.data),

  setServiceTypeActive: (id: number, active: boolean) =>
    apiClient
      .patch<ServiceType>(`/admin/service-types/${id}/status`, null, { params: { active } })
      .then((r) => r.data),
};

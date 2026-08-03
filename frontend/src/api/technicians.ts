import { apiClient } from './client';
import type { Technician } from '../types';

export const technicianApi = {
  getMyProfile: () => apiClient.get<Technician>('/technicians/me').then((r) => r.data),

  updateMyProfile: (params: { bio?: string; yearsExperience?: number }) =>
    apiClient.put<Technician>('/technicians/me', params).then((r) => r.data),

  setMyAvailability: (available: boolean) =>
    apiClient
      .patch<Technician>('/technicians/me/availability', null, { params: { available } })
      .then((r) => r.data),
};

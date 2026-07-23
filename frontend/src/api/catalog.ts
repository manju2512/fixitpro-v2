import { apiClient } from './client';
import type { ServiceType, Technician } from '../types';

export const catalogApi = {
  listServiceTypes: () => apiClient.get<ServiceType[]>('/service-types').then((r) => r.data),

  listTechnicians: (serviceTypeId?: number) =>
    apiClient
      .get<Technician[]>('/technicians', { params: serviceTypeId ? { serviceTypeId } : undefined })
      .then((r) => r.data),

  getTechnician: (id: number) => apiClient.get<Technician>(`/technicians/${id}`).then((r) => r.data),

  setMyAvailability: (available: boolean) =>
    apiClient.patch(`/technicians/me/availability`, null, { params: { available } }).then((r) => r.data),
};

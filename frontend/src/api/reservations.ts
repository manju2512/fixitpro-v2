import { apiClient } from './client';
import type { Reservation, ReservationStatus } from '../types';

export interface CreateReservationParams {
  serviceTypeId: number;
  technicianId?: number;
  reservationDate: string;
  timeSlot: string;
  address: string;
  telephone: string;
  comments?: string;
}

export const reservationsApi = {
  create: (params: CreateReservationParams) =>
    apiClient.post<Reservation>('/reservations', params).then((r) => r.data),

  myReservations: () => apiClient.get<Reservation[]>('/reservations/me').then((r) => r.data),

  myAssignedJobs: () =>
    apiClient.get<Reservation[]>('/reservations/technicians/me').then((r) => r.data),

  get: (id: number) => apiClient.get<Reservation>(`/reservations/${id}`).then((r) => r.data),

  updateStatus: (id: number, status: ReservationStatus) =>
    apiClient.patch<Reservation>(`/reservations/${id}/status`, { status }).then((r) => r.data),

  assignTechnician: (id: number, technicianId: number) =>
    apiClient.patch<Reservation>(`/reservations/${id}/assign`, { technicianId }).then((r) => r.data),

  adminAll: () => apiClient.get<Reservation[]>('/reservations/admin/all').then((r) => r.data),
};

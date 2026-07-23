import { apiClient } from './client';
import type { Review } from '../types';

export const reviewsApi = {
  create: (params: { reservationId: number; rating: number; comment: string }) =>
    apiClient.post<Review>('/reviews', params).then((r) => r.data),

  update: (id: number, params: { rating: number; comment: string }) =>
    apiClient.put<Review>(`/reviews/${id}`, params).then((r) => r.data),

  get: (id: number) => apiClient.get<Review>(`/reviews/${id}`).then((r) => r.data),

  forTechnician: (technicianId: number) =>
    apiClient.get<Review[]>(`/reviews/technician/${technicianId}`).then((r) => r.data),

  reply: (reviewId: number, replyText: string) =>
    apiClient.post<Review>(`/reviews/${reviewId}/reply`, { replyText }).then((r) => r.data),

  adminAll: () => apiClient.get<Review[]>('/reviews/admin/all').then((r) => r.data),

  moderateReply: (replyId: number, status: 'VISIBLE' | 'HIDDEN' | 'DELETED') =>
    apiClient
      .patch(`/admin/reviews/replies/${replyId}/moderate`, { status })
      .then((r) => r.data),
};

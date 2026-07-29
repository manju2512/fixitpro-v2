// Mirrors the DTOs actually returned by core-service. Keep these in sync
// with services/core-service/src/main/java/com/fixitpro/**/dto/*.java —
// this is the contract boundary between the two codebases.

export type Role = 'CUSTOMER' | 'TECHNICIAN' | 'ADMIN';

export type ReservationStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  userId: number;
  username: string;
  role: Role;
}

export interface ServiceType {
  serviceTypeId: number;
  name: string;
  description: string;
  basePrice: number;
  active: boolean;
}

export interface Technician {
  technicianId: number;
  name: string;
  serviceTypeId: number;
  serviceType: string;
  bio: string;
  yearsExperience: number;
  available: boolean;
  ratingAvg: number;
  ratingCount: number;
}

export interface Reservation {
  reservationId: number;
  customerId: number;
  customerName: string;
  technicianId: number | null;
  technicianName: string | null;
  serviceTypeId: number;
  serviceTypeName: string;
  reservationDate: string; // ISO date
  timeSlot: string;
  status: ReservationStatus;
  address: string;
  telephone: string;
  comments: string | null;
  createdAt?: string;
}

export interface ReviewReply {
  replyId: number;
  technicianId: number;
  technicianName: string;
  replyText: string;
  status: 'VISIBLE' | 'HIDDEN' | 'DELETED';
  createdAt?: string;
  updatedAt?: string;
}

export interface Review {
  reviewId: number;
  reservationId: number;
  customerId: number;
  customerName: string;
  technicianId: number;
  technicianName: string;
  rating: number;
  comment: string;
  edited: boolean;
  createdAt?: string;
  updatedAt?: string;
  reply: ReviewReply | null;
}

export interface UserSummary {
  userId: number;
  username: string;
  email: string;
  phone: string;
  role: Role;
  active: boolean;
  createdAt?: string;
}

export interface DashboardStats {
  totalCustomers: number;
  totalTechnicians: number;
  totalReservations: number;
  reservationsByStatus: Record<ReservationStatus, number>;
  totalReviews: number;
  averageRating: number;
}

// Mirrors services/ai-chat-service/.../chat/ChatMessageDto.java + ChatResponse.java
export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface ChatResponse {
  reply: string;
  messages: ChatMessage[];
}

export interface ApiErrorBody {
  message: string;
  status?: number;
  [key: string]: unknown;
}

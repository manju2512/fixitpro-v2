import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { AuthResponse } from '../types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

const ACCESS_TOKEN_KEY = 'fixitpro.accessToken';
const REFRESH_TOKEN_KEY = 'fixitpro.refreshToken';

export const tokenStorage = {
  getAccessToken: () => localStorage.getItem(ACCESS_TOKEN_KEY),
  getRefreshToken: () => localStorage.getItem(REFRESH_TOKEN_KEY),
  setTokens: (accessToken: string, refreshToken: string) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  },
  clear: () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },
};

// Queue concurrent requests while a single refresh is in flight, instead of
// firing one refresh call per failed request. Module-level so it's shared
// across every client this factory creates (core-service, ai-chat-service).
let refreshPromise: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) throw new Error('No refresh token available');

  const { data } = await axios.post<AuthResponse>(`${BASE_URL}/auth/refresh`, {
    refreshToken,
  });
  tokenStorage.setTokens(data.accessToken, data.refreshToken);
  return data.accessToken;
}

/**
 * Both backend services verify the same JWT (ai-chat-service never issues its own -
 * see services/ai-chat-service's JwtVerifier), so any client built here can reuse the
 * same access token and the same refresh-on-401 flow, just against a different baseURL.
 */
function createApiClient(baseURL: string) {
  const client = axios.create({ baseURL });

  client.interceptors.request.use((config) => {
    const token = tokenStorage.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  client.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
      const originalRequest = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;

      const isAuthEndpoint = originalRequest?.url?.includes('/auth/');
      if (error.response?.status === 401 && originalRequest && !originalRequest._retry && !isAuthEndpoint) {
        originalRequest._retry = true;
        try {
          refreshPromise ??= refreshAccessToken().finally(() => {
            refreshPromise = null;
          });
          const newAccessToken = await refreshPromise;
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
          return client(originalRequest);
        } catch (refreshError) {
          tokenStorage.clear();
          window.location.href = '/login';
          return Promise.reject(refreshError);
        }
      }

      return Promise.reject(error);
    },
  );

  return client;
}

export const apiClient = createApiClient(BASE_URL);

const CHAT_BASE_URL = import.meta.env.VITE_CHAT_API_BASE_URL ?? 'http://localhost:8081/api';
export const chatApiClient = createApiClient(CHAT_BASE_URL);

/** Pulls a human-readable message out of core-service's error response shape. */
export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const message = (error.response?.data as { message?: string } | undefined)?.message;
    if (message) return message;
    if (error.response?.status) return `Request failed (${error.response.status})`;
  }
  return 'Something went wrong. Please try again.';
}

/**
 * Pulls the per-field error map out of core-service's error response, if
 * present - covers both @Valid validation failures and duplicate-resource
 * conflicts (e.g. signing up with an email or phone that's already taken).
 * Returns undefined when the error isn't about a specific field, so callers
 * can fall back to the flat message from getApiErrorMessage().
 */
export function getApiFieldErrors(error: unknown): Record<string, string> | undefined {
  if (axios.isAxiosError(error)) {
    return (error.response?.data as { fieldErrors?: Record<string, string> } | undefined)?.fieldErrors;
  }
  return undefined;
}
import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { authApi } from '../api/auth';
import { tokenStorage } from '../api/client';
import type { Role } from '../types';

interface CurrentUser {
  userId: number;
  username: string;
  role: Role;
}

interface AuthContextValue {
  user: CurrentUser | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<CurrentUser>;
  signup: (params: { username: string; email: string; password: string; phone: string }) => Promise<CurrentUser>;
  logout: () => void;
}

const USER_KEY = 'fixitpro.user';

const AuthContext = createContext<AuthContextValue | null>(null);

function loadStoredUser(): CurrentUser | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as CurrentUser;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(loadStoredUser);

  const persist = useCallback((next: CurrentUser) => {
    localStorage.setItem(USER_KEY, JSON.stringify(next));
    setUser(next);
  }, []);

  const login = useCallback(
    async (username: string, password: string) => {
      const res = await authApi.login(username, password);
      tokenStorage.setTokens(res.accessToken, res.refreshToken);
      const next = { userId: res.userId, username: res.username, role: res.role };
      persist(next);
      return next;
    },
    [persist],
  );

  const signup = useCallback(
    async (params: { username: string; email: string; password: string; phone: string }) => {
      const res = await authApi.signup(params);
      tokenStorage.setTokens(res.accessToken, res.refreshToken);
      const next = { userId: res.userId, username: res.username, role: res.role };
      persist(next);
      return next;
    },
    [persist],
  );

  const logout = useCallback(() => {
    tokenStorage.clear();
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ user, isAuthenticated: user !== null, login, signup, logout }),
    [user, login, signup, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}

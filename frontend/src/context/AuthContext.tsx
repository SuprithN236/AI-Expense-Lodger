import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api } from '../api/client';
import { authApi } from '../api/endpoints';
import type { AuthResponse, User } from '../api/types';

const TOKEN_KEY = 'ai-expense-ledger.token';
const USER_KEY = 'ai-expense-ledger.user';

interface AuthContextValue {
  user: User | null;
  token: string | null;
  login: (email: string, password: string) => Promise<void>;
  signup: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function readStoredSession(): { token: string | null; user: User | null } {
  try {
    const token = localStorage.getItem(TOKEN_KEY);
    const rawUser = localStorage.getItem(USER_KEY);
    if (!token || !rawUser) return { token: null, user: null };
    return { token, user: JSON.parse(rawUser) as User };
  } catch {
    return { token: null, user: null };
  }
}

function persistSession(session: AuthResponse | null) {
  try {
    if (session) {
      localStorage.setItem(TOKEN_KEY, session.token);
      localStorage.setItem(USER_KEY, JSON.stringify(session.user));
    } else {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    }
  } catch {
    // Storage can be unavailable (private mode, quota); the in-memory session still works.
  }
}

// ---------------------------------------------------------------------------------------------
// Interceptors. Installed once at module load, before any component can issue a request.
// ---------------------------------------------------------------------------------------------

let currentToken: string | null = readStoredSession().token;
let onUnauthorized: (() => void) | null = null;

api.interceptors.request.use((config) => {
  if (currentToken) {
    config.headers.set('Authorization', `Bearer ${currentToken}`);
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthEndpoint = String(error?.config?.url ?? '').startsWith('/api/v1/auth/');
    if (error?.response?.status === 401 && !isAuthEndpoint) {
      onUnauthorized?.();
    }
    return Promise.reject(error);
  },
);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState(readStoredSession);

  const applySession = useCallback((response: AuthResponse | null) => {
    persistSession(response);
    currentToken = response?.token ?? null;
    setSession({ token: response?.token ?? null, user: response?.user ?? null });
  }, []);

  const logout = useCallback(() => applySession(null), [applySession]);

  useEffect(() => {
    // An expired or revoked token anywhere in the app returns the user to the login screen.
    onUnauthorized = logout;
    return () => {
      onUnauthorized = null;
    };
  }, [logout]);

  const login = useCallback(
    async (email: string, password: string) => applySession(await authApi.login(email, password)),
    [applySession],
  );

  const signup = useCallback(
    async (email: string, password: string) => applySession(await authApi.signup(email, password)),
    [applySession],
  );

  const value = useMemo<AuthContextValue>(
    () => ({ user: session.user, token: session.token, login, signup, logout }),
    [session, login, signup, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>');
  return context;
}

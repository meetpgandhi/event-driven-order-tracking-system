import { create } from 'zustand';
import type { UserSession } from '../types';

interface AuthState {
  session: UserSession | null;
  login: (session: UserSession) => void;
  logout: () => void;
  isAuthenticated: () => boolean;
  isAdmin: () => boolean;
  isAgent: () => boolean;
}

export const useAuthStore = create<AuthState>((set, get) => {
  const saved = localStorage.getItem('edots_user_session');
  let initialSession: UserSession | null = null;
  if (saved) {
    try {
      initialSession = JSON.parse(saved);
    } catch {
      localStorage.removeItem('edots_user_session');
    }
  }

  return {
    session: initialSession,
    login: (session: UserSession) => {
      localStorage.setItem('edots_user_session', JSON.stringify(session));
      set({ session });
    },
    logout: () => {
      localStorage.removeItem('edots_user_session');
      set({ session: null });
    },
    isAuthenticated: () => !!get().session?.accessToken,
    isAdmin: () => get().session?.role === 'ADMIN',
    isAgent: () => get().session?.role === 'AGENT'
  };
});

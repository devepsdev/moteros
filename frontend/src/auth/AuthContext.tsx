import * as authApi from "@/api/auth";
import { ApiError } from "@/api/client";
import * as usuariosApi from "@/api/usuarios";
import type { LoginRequest, LoginResponse, RegistroRequest, UsuarioResponse } from "@/types/dto";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  useSyncExternalStore,
  type ReactNode,
} from "react";
import { authStore } from "./authStore";

interface AuthContextValue {
  /** Perfil del usuario autenticado, o null si no hay sesión. */
  user: UsuarioResponse | null;
  isAuthenticated: boolean;
  /** true mientras se comprueba si hay una sesión guardada al arrancar. */
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  registro: (data: RegistroRequest) => Promise<void>;
  logout: () => Promise<void>;
  /** Vuelve a pedir /api/usuarios/me (tras editar el perfil, crear una moto, etc.). */
  refreshProfile: () => Promise<void>;
  /** Cierra la sesión local sin llamar al backend (p. ej. tras borrar la cuenta). */
  clearLocalSession: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const state = useSyncExternalStore(authStore.subscribe, authStore.getState, authStore.getState);
  const [isLoading, setIsLoading] = useState(true);

  const loadProfile = useCallback(async () => {
    try {
      authStore.setUser(await usuariosApi.miPerfil());
    } catch (error) {
      // Token ya no válido (contraseña cambiada en otro dispositivo, cuenta borrada...).
      if (error instanceof ApiError && (error.status === 401 || error.status === 403 || error.status === 404)) {
        await authStore.clearSession();
      }
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      await authStore.bootstrap();
      if (authStore.getState().refreshToken) {
        const refreshed = await authStore.refreshAccessToken();
        if (refreshed && !cancelled) await loadProfile();
      }
      if (!cancelled) setIsLoading(false);
    })();
    return () => {
      cancelled = true;
    };
  }, [loadProfile]);

  const startSession = useCallback(async (auth: LoginResponse) => {
    await authStore.setTokens(auth.token, auth.refreshToken);
    authStore.setUser(auth.usuario);
  }, []);

  const login = useCallback(async (credentials: LoginRequest) => startSession(await authApi.login(credentials)), [startSession]);

  const registro = useCallback(async (data: RegistroRequest) => startSession(await authApi.registro(data)), [startSession]);

  const logout = useCallback(async () => {
    const { refreshToken } = authStore.getState();
    await authStore.clearSession();
    if (refreshToken) {
      // Best-effort: si falla, la sesión local ya está cerrada igualmente.
      authApi.logout(refreshToken).catch(() => {});
    }
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user: state.user,
      isAuthenticated: state.user !== null,
      isLoading,
      login,
      registro,
      logout,
      refreshProfile: loadProfile,
      clearLocalSession: authStore.clearSession,
    }),
    [state.user, isLoading, login, registro, logout, loadProfile]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth debe usarse dentro de un <AuthProvider>");
  return context;
}

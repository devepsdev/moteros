import { API_URL } from "@/api/config";
import type { ApiResponse, LoginResponse, UsuarioResponse } from "@/types/dto";
import * as tokenStorage from "./tokenStorage";

const ACCESS_TOKEN_KEY = "moteros.accessToken";
const REFRESH_TOKEN_KEY = "moteros.refreshToken";

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: UsuarioResponse | null;
}

type Listener = () => void;

let state: AuthState = { accessToken: null, refreshToken: null, user: null };
const listeners = new Set<Listener>();

function notify(): void {
  for (const listener of listeners) listener();
}

function subscribe(listener: Listener): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

function getState(): AuthState {
  return state;
}

function getAccessToken(): string | null {
  return state.accessToken;
}

/** Carga los tokens persistidos al arrancar la app. No hace ninguna llamada de red. */
async function bootstrap(): Promise<void> {
  const [accessToken, refreshToken] = await Promise.all([
    tokenStorage.getItem(ACCESS_TOKEN_KEY),
    tokenStorage.getItem(REFRESH_TOKEN_KEY),
  ]);
  state = { ...state, accessToken, refreshToken };
  notify();
}

async function setTokens(accessToken: string, refreshToken: string): Promise<void> {
  state = { ...state, accessToken, refreshToken };
  notify();
  await Promise.all([
    tokenStorage.setItem(ACCESS_TOKEN_KEY, accessToken),
    tokenStorage.setItem(REFRESH_TOKEN_KEY, refreshToken),
  ]);
}

function setUser(user: UsuarioResponse | null): void {
  state = { ...state, user };
  notify();
}

async function clearSession(): Promise<void> {
  state = { accessToken: null, refreshToken: null, user: null };
  notify();
  await Promise.all([tokenStorage.deleteItem(ACCESS_TOKEN_KEY), tokenStorage.deleteItem(REFRESH_TOKEN_KEY)]);
}

let refreshPromise: Promise<boolean> | null = null;

/**
 * Renueva el access token con el refresh token guardado. Las llamadas simultáneas
 * comparten la misma renovación: el backend rota el refresh token en cada uso, así que
 * una segunda petición en paralelo lo invalidaría y cerraría la sesión.
 */
async function refreshAccessToken(): Promise<boolean> {
  const currentRefreshToken = state.refreshToken;
  if (!currentRefreshToken) return false;

  if (!refreshPromise) {
    refreshPromise = doRefresh(currentRefreshToken).finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

async function doRefresh(refreshToken: string): Promise<boolean> {
  try {
    const response = await fetch(`${API_URL}/api/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    if (!response.ok) {
      // Refresh caducado, revocado o ya rotado: la sesión ya no es válida.
      if (response.status === 400 || response.status === 401 || response.status === 403) {
        await clearSession();
      }
      return false;
    }
    const body = (await response.json()) as ApiResponse<LoginResponse>;
    await setTokens(body.data.token, body.data.refreshToken);
    return true;
  } catch {
    // Fallo de red: no cerramos sesión, puede que solo no haya conexión ahora mismo.
    return false;
  }
}

export const authStore = {
  subscribe,
  getState,
  getAccessToken,
  bootstrap,
  setTokens,
  setUser,
  clearSession,
  refreshAccessToken,
};

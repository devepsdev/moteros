import { authStore } from "@/auth/authStore";
import type { ApiResponse } from "@/types/dto";
import { API_URL } from "./config";

/** Error lanzado por `request()` para cualquier respuesta HTTP no exitosa. */
export class ApiError extends Error {
  readonly status: number;
  /** Errores de validación por campo (cuando el backend responde 400 "Error de validacion"). */
  readonly errores?: Record<string, string>;

  constructor(status: number, mensaje: string, errores?: Record<string, string>) {
    super(mensaje);
    this.name = "ApiError";
    this.status = status;
    this.errores = errores;
  }
}

type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

interface RequestOptions {
  method?: HttpMethod;
  /** Objeto que se envía como JSON, o FormData para subidas multipart. */
  body?: unknown;
  /** Parámetros de query string; los valores undefined/null/"" se omiten. */
  params?: Record<string, string | number | boolean | undefined | null>;
  /** Si es false, no se envía Authorization ni se intenta refrescar el token en un 401/403. */
  auth?: boolean;
}

function buildUrl(path: string, params?: RequestOptions["params"]): string {
  let url = `${API_URL}${path}`;
  if (params) {
    const query = Object.entries(params)
      .filter(([, value]) => value !== undefined && value !== null && value !== "")
      .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
      .join("&");
    if (query) url += `?${query}`;
  }
  return url;
}

async function rawRequest(path: string, options: RequestOptions): Promise<Response> {
  const { method = "GET", body, params, auth = true } = options;
  const headers: Record<string, string> = { Accept: "application/json" };
  const isForm = typeof FormData !== "undefined" && body instanceof FormData;

  if (body !== undefined && !isForm) {
    headers["Content-Type"] = "application/json";
  }
  if (auth) {
    const token = authStore.getAccessToken();
    if (token) headers.Authorization = `Bearer ${token}`;
  }

  return fetch(buildUrl(path, params), {
    method,
    headers,
    body: body === undefined ? undefined : isForm ? (body as FormData) : JSON.stringify(body),
  });
}

async function parseBody(response: Response): Promise<unknown> {
  if (response.status === 204) return undefined;
  const contentType = response.headers.get("content-type") ?? "";
  if (!contentType.includes("application/json")) return undefined;
  try {
    return await response.json();
  } catch {
    return undefined;
  }
}

/**
 * Llama a la API y devuelve `data` ya desenvuelto del `ApiResponseDTO` del backend.
 *
 * Sin token o con el token caducado, Spring Security responde 403 (no 401), así que
 * ante ambos códigos se intenta renovar el access token una vez y repetir la petición.
 */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  let response = await rawRequest(path, options);

  if ((response.status === 401 || response.status === 403) && options.auth !== false && authStore.getState().refreshToken) {
    const refreshed = await authStore.refreshAccessToken();
    if (refreshed) {
      response = await rawRequest(path, options);
    }
  }

  const body = (await parseBody(response)) as Partial<ApiResponse<unknown>> | undefined;

  if (!response.ok) {
    const data = body?.data;
    const errores =
      data && typeof data === "object" && !Array.isArray(data) ? (data as Record<string, string>) : undefined;
    throw new ApiError(response.status, mensajePorDefecto(response.status, body?.message), errores);
  }

  return body?.data as T;
}

function mensajePorDefecto(status: number, message?: string): string {
  if (message && !message.startsWith("Error interno del servidor")) return message;
  if (status === 403) return "No tienes permiso para hacer esto.";
  if (status >= 500) return "El servidor ha tenido un problema. Inténtalo de nuevo en un momento.";
  return "Ha ocurrido un error inesperado.";
}

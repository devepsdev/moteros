import type { PageResponse, UsuarioSummary } from "@/types/dto";
import { request } from "./client";

/** Bloquea al usuario: deja de haber amistad, mensajes y contenido visible entre los dos. */
export function bloquear(usuarioUuid: string): Promise<void> {
  return request<void>(`/api/bloqueos/${usuarioUuid}`, { method: "POST" });
}

export function desbloquear(usuarioUuid: string): Promise<void> {
  return request<void>(`/api/bloqueos/${usuarioUuid}`, { method: "DELETE" });
}

export function misBloqueados(page: number, size = 20): Promise<PageResponse<UsuarioSummary>> {
  return request<PageResponse<UsuarioSummary>>("/api/bloqueos", { params: { page, size } });
}

/** Si he bloqueado a este usuario (no dice si él me ha bloqueado a mí). */
export async function heBloqueado(usuarioUuid: string): Promise<boolean> {
  const res = await request<{ bloqueado: boolean }>(`/api/bloqueos/${usuarioUuid}`);
  return res.bloqueado;
}

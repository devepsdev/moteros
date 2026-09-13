import type { Amistad, PageResponse, UsuarioSummary } from "@/types/dto";
import { request } from "./client";

export function enviarSolicitud(usuarioUuid: string): Promise<Amistad> {
  return request<Amistad>(`/api/amistades/${usuarioUuid}`, { method: "POST" });
}

export function responder(amistadUuid: string, aceptar: boolean): Promise<Amistad> {
  return request<Amistad>(`/api/amistades/${amistadUuid}`, { method: "PATCH", params: { aceptar } });
}

/** Borra la relación con el usuario: deja de ser amigo o retira la solicitud. */
export function eliminar(usuarioUuid: string): Promise<void> {
  return request<void>(`/api/amistades/${usuarioUuid}`, { method: "DELETE" });
}

export function amigos(usuarioUuid: string, page: number, size = 20): Promise<PageResponse<UsuarioSummary>> {
  return request<PageResponse<UsuarioSummary>>(`/api/amistades/usuario/${usuarioUuid}/amigos`, { params: { page, size } });
}

export function recibidas(page: number, size = 20): Promise<PageResponse<Amistad>> {
  return request<PageResponse<Amistad>>("/api/amistades/solicitudes/recibidas", { params: { page, size } });
}

export function enviadas(page: number, size = 20): Promise<PageResponse<Amistad>> {
  return request<PageResponse<Amistad>>("/api/amistades/solicitudes/enviadas", { params: { page, size } });
}

/** Relación con otro usuario, o null si no hay ninguna. */
export function relacionCon(usuarioUuid: string): Promise<Amistad | null> {
  return request<Amistad | null>(`/api/amistades/relacion/${usuarioUuid}`);
}

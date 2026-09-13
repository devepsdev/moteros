import type {
  EstadoInscripcion,
  EstadoQuedada,
  InscripcionQuedada,
  PageResponse,
  QuedadaRequest,
  QuedadaResponse,
  QuedadaSummary,
} from "@/types/dto";
import { request } from "./client";

/** Quedadas programadas a partir de ahora, las más cercanas en el tiempo primero. */
export function proximas(page: number, size = 20): Promise<PageResponse<QuedadaSummary>> {
  return request<PageResponse<QuedadaSummary>>("/api/quedadas/filtrar", {
    params: { estado: "programada", soloProximas: true, page, size, sortBy: "fechaHora", sortDir: "asc" },
  });
}

export function misInscripciones(page: number, size = 20): Promise<PageResponse<QuedadaSummary>> {
  return request<PageResponse<QuedadaSummary>>("/api/quedadas/mis-inscripciones", {
    params: { page, size, sortBy: "fechaHora", sortDir: "desc" },
  });
}

export function deOrganizador(organizadorUuid: string, page: number, size = 20): Promise<PageResponse<QuedadaSummary>> {
  return request<PageResponse<QuedadaSummary>>(`/api/quedadas/organizador/${organizadorUuid}`, {
    params: { page, size, sortBy: "fechaHora", sortDir: "desc" },
  });
}

export function obtener(uuid: string): Promise<QuedadaResponse> {
  return request<QuedadaResponse>(`/api/quedadas/${uuid}`);
}

export function crear(data: QuedadaRequest): Promise<QuedadaResponse> {
  return request<QuedadaResponse>("/api/quedadas", { method: "POST", body: data });
}

export function actualizar(uuid: string, data: QuedadaRequest): Promise<QuedadaResponse> {
  return request<QuedadaResponse>(`/api/quedadas/${uuid}`, { method: "PUT", body: data });
}

export function cambiarEstado(uuid: string, estado: EstadoQuedada): Promise<QuedadaResponse> {
  return request<QuedadaResponse>(`/api/quedadas/${uuid}/estado`, { method: "PATCH", params: { estado } });
}

export function eliminar(uuid: string): Promise<void> {
  return request<void>(`/api/quedadas/${uuid}`, { method: "DELETE" });
}

export function inscribirse(uuid: string): Promise<InscripcionQuedada> {
  return request<InscripcionQuedada>(`/api/quedadas/${uuid}/inscripciones`, { method: "POST" });
}

export function cancelarInscripcion(uuid: string): Promise<void> {
  return request<void>(`/api/quedadas/${uuid}/inscripciones`, { method: "DELETE" });
}

export function cambiarEstadoInscripcion(uuid: string, usuarioUuid: string, estado: EstadoInscripcion): Promise<InscripcionQuedada> {
  return request<InscripcionQuedada>(`/api/quedadas/${uuid}/inscripciones/${usuarioUuid}`, {
    method: "PATCH",
    params: { estado },
  });
}

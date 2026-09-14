import type { EstadisticasGlobales, PageResponse, RolUsuario, UsuarioResponse } from "@/types/dto";
import { request } from "./client";

export function estadisticas(): Promise<EstadisticasGlobales> {
  return request<EstadisticasGlobales>("/api/admin/estadisticas");
}

/** Usuarios de la plataforma, los más recientes primero. */
export function usuarios(texto: string, page: number, size = 20): Promise<PageResponse<UsuarioResponse>> {
  return request<PageResponse<UsuarioResponse>>("/api/admin/usuarios", { params: { texto, page, size } });
}

export function cambiarRol(uuid: string, rol: RolUsuario): Promise<UsuarioResponse> {
  return request<UsuarioResponse>(`/api/admin/usuarios/${uuid}/rol`, { method: "PATCH", params: { rol } });
}

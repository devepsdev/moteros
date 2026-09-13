import type { PageResponse, UsuarioRequest, UsuarioResponse, UsuarioSummary } from "@/types/dto";
import { request } from "./client";

export function miPerfil(): Promise<UsuarioResponse> {
  return request<UsuarioResponse>("/api/usuarios/me");
}

export function actualizarPerfil(data: UsuarioRequest): Promise<UsuarioResponse> {
  return request<UsuarioResponse>("/api/usuarios/me", { method: "PUT", body: data });
}

export function eliminarMiCuenta(): Promise<void> {
  return request<void>("/api/usuarios/me", { method: "DELETE" });
}

export function obtener(uuid: string): Promise<UsuarioResponse> {
  return request<UsuarioResponse>(`/api/usuarios/${uuid}`);
}

export function buscar(texto: string, page: number, size = 20): Promise<PageResponse<UsuarioSummary>> {
  return request<PageResponse<UsuarioSummary>>("/api/usuarios", { params: { searchText: texto, page, size } });
}

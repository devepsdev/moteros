import type { MotoRequest, MotoResponse } from "@/types/dto";
import { request } from "./client";

export function mias(): Promise<MotoResponse[]> {
  return request<MotoResponse[]>("/api/motos/mias");
}

export function deUsuario(usuarioUuid: string): Promise<MotoResponse[]> {
  return request<MotoResponse[]>(`/api/motos/usuario/${usuarioUuid}`);
}

export function obtener(uuid: string): Promise<MotoResponse> {
  return request<MotoResponse>(`/api/motos/${uuid}`);
}

export function crear(data: MotoRequest): Promise<MotoResponse> {
  return request<MotoResponse>("/api/motos", { method: "POST", body: data });
}

export function actualizar(uuid: string, data: MotoRequest): Promise<MotoResponse> {
  return request<MotoResponse>(`/api/motos/${uuid}`, { method: "PUT", body: data });
}

export function eliminar(uuid: string): Promise<void> {
  return request<void>(`/api/motos/${uuid}`, { method: "DELETE" });
}

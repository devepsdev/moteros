import type { Notificacion, PageResponse } from "@/types/dto";
import { request } from "./client";

export function listar(page: number, size = 20): Promise<PageResponse<Notificacion>> {
  return request<PageResponse<Notificacion>>("/api/notificaciones", { params: { page, size } });
}

export async function contarNoLeidas(): Promise<number> {
  const res = await request<{ noLeidas: number }>("/api/notificaciones/no-leidas/contador");
  return res.noLeidas;
}

export function marcarLeida(uuid: string): Promise<void> {
  return request<void>(`/api/notificaciones/${uuid}/leida`, { method: "PATCH" });
}

export function marcarTodasLeidas(): Promise<void> {
  return request<void>("/api/notificaciones/leidas", { method: "PATCH" });
}

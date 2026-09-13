import type { Conversacion, Mensaje, PageResponse } from "@/types/dto";
import { request } from "./client";

export function conversaciones(page: number, size = 20): Promise<PageResponse<Conversacion>> {
  return request<PageResponse<Conversacion>>("/api/chat/conversaciones", { params: { page, size } });
}

export function obtener(uuid: string): Promise<Conversacion> {
  return request<Conversacion>(`/api/chat/conversaciones/${uuid}`);
}

/** Mensajes del más reciente al más antiguo. Al pedirlos, el backend los marca como leídos. */
export function mensajes(uuid: string, page: number, size = 30): Promise<PageResponse<Mensaje>> {
  return request<PageResponse<Mensaje>>(`/api/chat/conversaciones/${uuid}/mensajes`, { params: { page, size } });
}

export function enviar(conversacionUuid: string, contenido: string): Promise<Mensaje> {
  return request<Mensaje>(`/api/chat/conversaciones/${conversacionUuid}/mensajes`, { method: "POST", body: { contenido } });
}

/** Mensaje a un usuario: crea la conversación si todavía no existe. */
export function enviarAUsuario(usuarioUuid: string, contenido: string): Promise<Mensaje> {
  return request<Mensaje>(`/api/chat/usuarios/${usuarioUuid}/mensajes`, { method: "POST", body: { contenido } });
}

export async function totalNoLeidos(): Promise<number> {
  const res = await request<{ total: number }>("/api/chat/no-leidos");
  return res.total;
}

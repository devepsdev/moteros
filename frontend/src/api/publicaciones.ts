import type { Comentario, PageResponse, Publicacion, PublicacionRequest } from "@/types/dto";
import { request } from "./client";

const ORDEN = { sortBy: "fechaPublicacion", sortDir: "desc" } as const;

/** Publicaciones propias y de amigos. */
export function feed(page: number, size = 15): Promise<PageResponse<Publicacion>> {
  return request<PageResponse<Publicacion>>("/api/publicaciones/feed", { params: { page, size, ...ORDEN } });
}

export function deUsuario(usuarioUuid: string, page: number, size = 15): Promise<PageResponse<Publicacion>> {
  return request<PageResponse<Publicacion>>(`/api/publicaciones/usuario/${usuarioUuid}`, {
    params: { page, size, ...ORDEN },
  });
}

export function obtener(uuid: string): Promise<Publicacion> {
  return request<Publicacion>(`/api/publicaciones/${uuid}`);
}

export function crear(data: PublicacionRequest): Promise<Publicacion> {
  return request<Publicacion>("/api/publicaciones", { method: "POST", body: data });
}

export function eliminar(uuid: string): Promise<void> {
  return request<void>(`/api/publicaciones/${uuid}`, { method: "DELETE" });
}

export function comentarios(uuid: string, page: number, size = 30): Promise<PageResponse<Comentario>> {
  return request<PageResponse<Comentario>>(`/api/publicaciones/${uuid}/comentarios`, {
    params: { page, size, sortBy: "fecha", sortDir: "asc" },
  });
}

export function comentar(uuid: string, contenido: string): Promise<Comentario> {
  return request<Comentario>(`/api/publicaciones/${uuid}/comentarios`, { method: "POST", body: { contenido } });
}

export function eliminarComentario(comentarioUuid: string): Promise<void> {
  return request<void>(`/api/publicaciones/comentarios/${comentarioUuid}`, { method: "DELETE" });
}

/** Alterna el "me gusta"; devuelve si queda con like. */
export async function alternarLike(uuid: string): Promise<boolean> {
  const res = await request<{ like: boolean }>(`/api/publicaciones/${uuid}/like`, { method: "PUT" });
  return res.like;
}

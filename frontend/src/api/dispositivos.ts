import { request } from "./client";

/** Da de alta el móvil para recibir avisos con la app cerrada. */
export function registrar(token: string, plataforma: string): Promise<void> {
  return request<void>("/api/dispositivos", { method: "POST", body: { token, plataforma } });
}

/** Baja del móvil, al cerrar sesión. */
export function eliminar(token: string): Promise<void> {
  return request<void>("/api/dispositivos", { method: "DELETE", body: { token } });
}

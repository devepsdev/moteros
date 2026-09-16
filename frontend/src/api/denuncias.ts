import { request } from "./client";

export type TipoDenuncia = "usuario" | "publicacion" | "comentario" | "mensaje" | "ruta" | "quedada";
export type MotivoDenuncia = "spam" | "acoso" | "odio" | "sexual" | "violencia" | "suplantacion" | "otro";

export const MOTIVOS_DENUNCIA: { value: MotivoDenuncia; label: string }[] = [
  { value: "spam", label: "Spam o publicidad" },
  { value: "acoso", label: "Acoso o insultos" },
  { value: "odio", label: "Odio o discriminación" },
  { value: "sexual", label: "Contenido sexual" },
  { value: "violencia", label: "Violencia o peligro" },
  { value: "suplantacion", label: "Suplantación" },
  { value: "otro", label: "Otro motivo" },
];

export function denunciar(dto: { tipo: TipoDenuncia; referenciaUuid: string; motivo: MotivoDenuncia; descripcion?: string }): Promise<void> {
  return request<void>("/api/denuncias", { method: "POST", body: dto });
}

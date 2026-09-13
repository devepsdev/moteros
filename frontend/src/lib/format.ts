import type { Dificultad, NivelRecomendado, TipoMoto, TipoTerreno } from "@/types/dto";

export const DIFICULTADES: { value: Dificultad; label: string }[] = [
  { value: "facil", label: "Fácil" },
  { value: "moderada", label: "Moderada" },
  { value: "dificil", label: "Difícil" },
  { value: "extrema", label: "Extrema" },
];

export const TERRENOS: { value: TipoTerreno; label: string }[] = [
  { value: "asfalto", label: "Asfalto" },
  { value: "offroad", label: "Off-road" },
  { value: "mixto", label: "Mixto" },
];

export const TIPOS_MOTO: { value: TipoMoto; label: string }[] = [
  { value: "naked", label: "Naked" },
  { value: "trail", label: "Trail" },
  { value: "custom", label: "Custom" },
  { value: "deportiva", label: "Deportiva" },
  { value: "touring", label: "Touring" },
  { value: "scooter", label: "Scooter" },
  { value: "clasica", label: "Clásica" },
];

export const NIVELES: { value: NivelRecomendado; label: string }[] = [
  { value: "cualquiera", label: "Cualquier nivel" },
  { value: "iniciacion", label: "Iniciación" },
  { value: "experimentado", label: "Experimentado" },
];

export const etiquetaNivel = (n: NivelRecomendado) => NIVELES.find((x) => x.value === n)?.label ?? n;

export const etiquetaDificultad = (d: Dificultad) => DIFICULTADES.find((x) => x.value === d)?.label ?? d;
export const etiquetaTerreno = (t: TipoTerreno) => TERRENOS.find((x) => x.value === t)?.label ?? t;
export const etiquetaTipoMoto = (t: TipoMoto) => TIPOS_MOTO.find((x) => x.value === t)?.label ?? t;

export function formatKm(km: number | null | undefined): string {
  if (km === null || km === undefined) return "—";
  return `${km.toLocaleString("es-ES", { maximumFractionDigits: 1 })} km`;
}

export function formatDuracion(min: number | null | undefined): string {
  if (!min) return "—";
  const h = Math.floor(min / 60);
  const m = min % 60;
  if (h === 0) return `${m} min`;
  return m === 0 ? `${h} h` : `${h} h ${m} min`;
}

/** Fecha relativa corta: "ahora", "hace 5 min", "hace 3 h", "hace 2 d" o "12 sep". */
export function formatRelativo(iso: string): string {
  const fecha = new Date(iso);
  const diffSeg = Math.round((Date.now() - fecha.getTime()) / 1000);
  if (diffSeg < 60) return "ahora";
  if (diffSeg < 3600) return `hace ${Math.floor(diffSeg / 60)} min`;
  if (diffSeg < 86400) return `hace ${Math.floor(diffSeg / 3600)} h`;
  if (diffSeg < 7 * 86400) return `hace ${Math.floor(diffSeg / 86400)} d`;
  return fecha.toLocaleDateString("es-ES", { day: "numeric", month: "short" });
}

export function iniciales(nombre: string): string {
  return nombre
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? "")
    .join("");
}

/** Distancia en km entre dos coordenadas (Haversine). */
export function distanciaKm(a: { latitud: number; longitud: number }, b: { latitud: number; longitud: number }): number {
  const R = 6371;
  const toRad = (g: number) => (g * Math.PI) / 180;
  const dLat = toRad(b.latitud - a.latitud);
  const dLon = toRad(b.longitud - a.longitud);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(a.latitud)) * Math.cos(toRad(b.latitud)) * Math.sin(dLon / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(h));
}

/** Longitud total de un recorrido por sus puntos, redondeada a 1 decimal. */
export function longitudTrack(puntos: { latitud: number; longitud: number }[]): number {
  let total = 0;
  for (let i = 1; i < puntos.length; i++) total += distanciaKm(puntos[i - 1], puntos[i]);
  return Math.round(total * 10) / 10;
}

/**
 * El backend trabaja con LocalDateTime en hora de España y sin zona ("2026-09-20T09:30:00").
 * JavaScript interpreta ese formato como hora local, que es la del usuario en España.
 */
export function fechaDeApi(iso: string): Date {
  return new Date(iso);
}

/** Fecha local a LocalDateTime del backend, sin zona y sin milisegundos. */
export function fechaParaApi(fecha: Date): string {
  const p = (n: number) => String(n).padStart(2, "0");
  return `${fecha.getFullYear()}-${p(fecha.getMonth() + 1)}-${p(fecha.getDate())}T${p(fecha.getHours())}:${p(fecha.getMinutes())}:00`;
}

/** "sáb 20 sep" */
export function formatDia(fecha: Date): string {
  return fecha.toLocaleDateString("es-ES", { weekday: "short", day: "numeric", month: "short" });
}

/** "09:30" */
export function formatHora(fecha: Date): string {
  return fecha.toLocaleTimeString("es-ES", { hour: "2-digit", minute: "2-digit" });
}

/** "sáb 20 sep · 09:30" */
export function formatFechaHora(iso: string): string {
  const fecha = fechaDeApi(iso);
  return `${formatDia(fecha)} · ${formatHora(fecha)}`;
}

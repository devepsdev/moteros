import { Dificultad, EstadoDenuncia, EstadoSugerencia, MotivoDenuncia, RolUsuario, TipoDenuncia, TipoTerreno } from '../models/api.model';

export const DIFICULTADES: { value: Dificultad; label: string }[] = [
  { value: 'facil', label: 'Fácil' },
  { value: 'moderada', label: 'Moderada' },
  { value: 'dificil', label: 'Difícil' },
  { value: 'extrema', label: 'Extrema' },
];

export const TERRENOS: { value: TipoTerreno; label: string }[] = [
  { value: 'asfalto', label: 'Asfalto' },
  { value: 'offroad', label: 'Off-road' },
  { value: 'mixto', label: 'Mixto' },
];

export const ROLES: { value: RolUsuario; label: string }[] = [
  { value: 'user', label: 'Usuario' },
  { value: 'admin', label: 'Administrador' },
  { value: 'scraper', label: 'Scraper' },
];

export const ESTADO_SUGERENCIA_LABELS: Record<EstadoSugerencia, string> = {
  pendiente: 'Pendiente',
  aprobada: 'Aprobada',
  rechazada: 'Rechazada',
};

export const TIPO_DENUNCIA_LABELS: Record<TipoDenuncia, string> = {
  usuario: 'Perfil',
  publicacion: 'Publicación',
  comentario: 'Comentario',
  mensaje: 'Mensaje privado',
  ruta: 'Ruta',
  quedada: 'Quedada',
};

export const MOTIVO_DENUNCIA_LABELS: Record<MotivoDenuncia, string> = {
  spam: 'Spam o publicidad',
  acoso: 'Acoso o insultos',
  odio: 'Odio o discriminación',
  sexual: 'Contenido sexual',
  violencia: 'Violencia o peligro',
  suplantacion: 'Suplantación',
  otro: 'Otro motivo',
};

export const ESTADO_DENUNCIA_LABELS: Record<EstadoDenuncia, string> = {
  pendiente: 'Pendiente',
  resuelta: 'Resuelta',
  descartada: 'Descartada',
};

export const etiquetaDificultad = (d: Dificultad | null) => DIFICULTADES.find((x) => x.value === d)?.label ?? 'Sin indicar';
export const etiquetaTerreno = (t: TipoTerreno | null) => TERRENOS.find((x) => x.value === t)?.label ?? 'Sin indicar';

/** "48,5 km" o guion si no hay dato. */
export function formatKm(km: number | null): string {
  return km == null ? '—' : `${km.toLocaleString('es-ES', { maximumFractionDigits: 1 })} km`;
}

/** "1 h 30 min". */
export function formatDuracion(min: number | null): string {
  if (!min) return '—';
  const h = Math.floor(min / 60);
  const m = min % 60;
  if (h === 0) return `${m} min`;
  return m === 0 ? `${h} h` : `${h} h ${m} min`;
}

/** Distancia en km entre dos coordenadas (Haversine). */
function distanciaKm(a: { latitud: number; longitud: number }, b: { latitud: number; longitud: number }): number {
  const R = 6371;
  const rad = (g: number) => (g * Math.PI) / 180;
  const dLat = rad(b.latitud - a.latitud);
  const dLon = rad(b.longitud - a.longitud);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.latitud)) * Math.cos(rad(b.latitud)) * Math.sin(dLon / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(h));
}

/** Longitud del recorrido por sus puntos, redondeada a 1 decimal. */
export function longitudTrack(puntos: { latitud: number; longitud: number }[]): number {
  let total = 0;
  for (let i = 1; i < puntos.length; i++) total += distanciaKm(puntos[i - 1], puntos[i]);
  return Math.round(total * 10) / 10;
}

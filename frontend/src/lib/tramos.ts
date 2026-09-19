import type { PuntoRuta } from "@/types/dto";

/**
 * Los puntos de una ruta son los que marca el usuario más los puntos de paso (`via`) que fijan
 * la carretera elegida entre dos de ellos. Estas funciones mantienen esa lista.
 */

const reordenar = (puntos: PuntoRuta[]) => puntos.map((p, orden) => ({ ...p, orden }));

/** Índice del último punto marcado por el usuario, o -1. */
export function ultimoMarcado(puntos: PuntoRuta[]): number {
  for (let i = puntos.length - 1; i >= 0; i--) if (!puntos[i].via) return i;
  return -1;
}

/**
 * Sustituye los puntos de paso entre `desde` y `hasta` (índices de dos puntos marcados
 * seguidos) por los de la carretera elegida. Devuelve la lista nueva y el nuevo índice de `hasta`.
 */
export function elegirCarretera(
  puntos: PuntoRuta[],
  desde: number,
  hasta: number,
  paso: { latitud: number; longitud: number }[]
): { puntos: PuntoRuta[]; hasta: number } {
  const via = paso.map((p) => ({ orden: 0, latitud: p.latitud, longitud: p.longitud, via: true }));
  const lista = [...puntos.slice(0, desde + 1), ...via, ...puntos.slice(hasta)];
  return { puntos: reordenar(lista), hasta: desde + via.length + 1 };
}

/** Quita el último punto marcado junto con los puntos de paso que llevaban a él. */
export function quitarUltimo(puntos: PuntoRuta[]): PuntoRuta[] {
  const ultimo = ultimoMarcado(puntos);
  if (ultimo < 0) return [];
  const anterior = ultimoMarcado(puntos.slice(0, ultimo));
  return reordenar(puntos.slice(0, anterior + 1));
}

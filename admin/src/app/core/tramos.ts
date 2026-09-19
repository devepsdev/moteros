/**
 * Los puntos de una ruta son los que marca el administrador más los puntos de paso (`via`) que
 * fijan la carretera elegida entre dos de ellos. Estas funciones mantienen esa lista.
 */
export interface PuntoConVia {
  latitud: number;
  longitud: number;
  nombre?: string | null;
  via?: boolean;
}

/** Índices de los puntos marcados (no de paso), en orden. */
export function indicesMarcados(puntos: PuntoConVia[]): number[] {
  return puntos.flatMap((p, i) => (p.via ? [] : [i]));
}

/**
 * Sustituye los puntos de paso entre `desde` y `hasta` (índices de dos puntos marcados
 * seguidos) por los de la carretera elegida.
 */
export function elegirCarretera<T extends PuntoConVia>(
  puntos: T[],
  desde: number,
  hasta: number,
  paso: { latitud: number; longitud: number }[],
): PuntoConVia[] {
  const via = paso.map((p) => ({ latitud: p.latitud, longitud: p.longitud, via: true }));
  return [...puntos.slice(0, desde + 1), ...via, ...puntos.slice(hasta)];
}

/** Quita el último punto marcado junto con los puntos de paso que llevaban a él. */
export function quitarUltimo<T extends PuntoConVia>(puntos: T[]): T[] {
  const marcados = indicesMarcados(puntos);
  if (marcados.length <= 1) return [];
  return puntos.slice(0, marcados[marcados.length - 2] + 1);
}

import * as Location from "expo-location";
import { useCallback, useState } from "react";

export interface Coordenadas {
  latitud: number;
  longitud: number;
}

/** Distancia en línea recta entre dos puntos, en km (Haversine, radio de la Tierra 6371 km). */
export function distanciaKm(a: Coordenadas, b: Coordenadas): number {
  const rad = Math.PI / 180;
  const dLat = (b.latitud - a.latitud) * rad;
  const dLon = (b.longitud - a.longitud) * rad;
  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(a.latitud * rad) * Math.cos(b.latitud * rad) * Math.sin(dLon / 2) ** 2;
  return 2 * 6371 * Math.asin(Math.min(1, Math.sqrt(h)));
}

/** Dónde está el usuario, o null si no da permiso o el GPS no responde. */
export async function posicionActual(): Promise<Coordenadas | null> {
  try {
    const { granted } = await Location.requestForegroundPermissionsAsync();
    if (!granted) return null;
    const pos =
      (await Location.getLastKnownPositionAsync()) ??
      (await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced }));
    return pos ? { latitud: pos.coords.latitude, longitud: pos.coords.longitude } : null;
  } catch {
    return null;
  }
}

/**
 * Ubicación bajo demanda: se pide la primera vez que se activa y se guarda mientras dure la
 * pantalla. `denegada` distingue el «he dicho que no» del «todavía no lo he pedido».
 */
export function useUbicacion() {
  const [coords, setCoords] = useState<Coordenadas | null>(null);
  const [cargando, setCargando] = useState(false);
  const [denegada, setDenegada] = useState(false);

  const pedir = useCallback(async (): Promise<Coordenadas | null> => {
    if (coords) return coords;
    setCargando(true);
    const pos = await posicionActual();
    setCargando(false);
    setDenegada(pos === null);
    setCoords(pos);
    return pos;
  }, [coords]);

  return { coords, cargando, denegada, pedir };
}

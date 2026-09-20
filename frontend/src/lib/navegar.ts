import { Alert, Linking } from "react-native";

interface Punto {
  latitud: number;
  longitud: number;
}

/** Google Maps admite como mucho nueve paradas intermedias en un enlace de indicaciones. */
const MAX_PARADAS = 9;

const coord = (p: Punto) => `${p.latitud.toFixed(6)},${p.longitud.toFixed(6)}`;

/**
 * Paradas intermedias repartidas por el recorrido. Si hay más de las que Google acepta, se cogen
 * a intervalos regulares para que el trazado se parezca al de la app y no se vaya por otra
 * carretera.
 */
function paradas(intermedios: Punto[]): Punto[] {
  if (intermedios.length <= MAX_PARADAS) return intermedios;
  const paso = (intermedios.length - 1) / (MAX_PARADAS - 1);
  return Array.from({ length: MAX_PARADAS }, (_, i) => intermedios[Math.round(i * paso)]);
}

/**
 * Enlace de indicaciones en coche de Google Maps para el recorrido. Devuelve null si no hay al
 * menos salida y llegada.
 */
export function urlGoogleMaps(puntos: Punto[]): string | null {
  if (puntos.length < 2) return null;
  const origen = puntos[0];
  const destino = puntos[puntos.length - 1];
  const intermedios = paradas(puntos.slice(1, -1));
  const params = [
    "api=1",
    "travelmode=driving",
    `origin=${coord(origen)}`,
    `destination=${coord(destino)}`,
  ];
  if (intermedios.length > 0) {
    params.push(`waypoints=${intermedios.map(coord).join("|")}`);
  }
  return `https://www.google.com/maps/dir/?${params.join("&")}`;
}

/**
 * Abre el recorrido en Google Maps (o en el navegador si no está instalado, que es lo que hace
 * Android con este enlace).
 */
export async function abrirEnGoogleMaps(puntos: Punto[]): Promise<void> {
  const url = urlGoogleMaps(puntos);
  if (!url) {
    Alert.alert("Sin recorrido", "Esta ruta no tiene marcados la salida y la llegada.");
    return;
  }
  try {
    await Linking.openURL(url);
  } catch {
    Alert.alert("No se ha podido abrir", "Instala Google Maps o inténtalo desde el navegador.");
  }
}

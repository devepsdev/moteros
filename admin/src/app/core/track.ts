/**
 * Lectura de recorridos GPX y KML en el navegador y simplificación del trazado.
 *
 * Un GPX de una ruta en moto trae miles de puntos (el de Les Guilleries, 16.000). La app solo
 * necesita la forma del recorrido, así que se reduce con Ramer-Douglas-Peucker conservando las
 * curvas y descartando los puntos casi alineados.
 */

export interface PuntoTrack {
  latitud: number;
  longitud: number;
}

/** Tope de puntos por ruta: dibuja bien las curvas sin cargar la app ni la base de datos. */
export const MAX_PUNTOS_RUTA = 300;

/** Lee los puntos de un fichero GPX (track, ruta o waypoints) o KML (LineString o puntos). */
export function leerTrack(contenido: string, nombreFichero: string): PuntoTrack[] {
  const doc = new DOMParser().parseFromString(contenido, 'application/xml');
  if (doc.getElementsByTagName('parsererror').length > 0) {
    throw new Error(`«${nombreFichero}» no es un GPX o KML válido.`);
  }
  const puntos = nombreFichero.toLowerCase().endsWith('.kml') || doc.getElementsByTagName('kml').length > 0 ? leerKml(doc) : leerGpx(doc);
  if (puntos.length < 2) {
    throw new Error(`«${nombreFichero}» no contiene un recorrido (menos de 2 puntos).`);
  }
  return puntos;
}

function leerGpx(doc: Document): PuntoTrack[] {
  // Preferencia: track grabado; si no hay, ruta planificada; si tampoco, waypoints sueltos.
  for (const etiqueta of ['trkpt', 'rtept', 'wpt']) {
    const nodos = Array.from(doc.getElementsByTagName(etiqueta));
    const puntos = nodos.map((n) => valido(Number(n.getAttribute('lat')), Number(n.getAttribute('lon')))).filter(esPunto);
    if (puntos.length >= 2) return puntos;
  }
  return [];
}

function leerKml(doc: Document): PuntoTrack[] {
  // <coordinates> lleva "lon,lat[,alt]" separados por espacios; se unen todas las líneas en orden.
  const lineas = Array.from(doc.getElementsByTagName('LineString'));
  const fuentes = lineas.length > 0 ? lineas : Array.from(doc.getElementsByTagName('Placemark'));
  const puntos: PuntoTrack[] = [];
  for (const fuente of fuentes) {
    for (const coords of Array.from(fuente.getElementsByTagName('coordinates'))) {
      for (const tupla of (coords.textContent ?? '').trim().split(/\s+/)) {
        const [lon, lat] = tupla.split(',').map(Number);
        const punto = valido(lat, lon);
        if (punto) puntos.push(punto);
      }
    }
  }
  return puntos;
}

function valido(lat: number, lon: number): PuntoTrack | null {
  return Number.isFinite(lat) && Number.isFinite(lon) && Math.abs(lat) <= 90 && Math.abs(lon) <= 180
    ? { latitud: lat, longitud: lon }
    : null;
}

function esPunto(p: PuntoTrack | null): p is PuntoTrack {
  return p !== null;
}

/**
 * Simplifica el trazado a como mucho `maximo` puntos. Busca la tolerancia más pequeña que
 * cumple el tope, así se conserva todo el detalle posible. Siempre mantiene el primero y el último.
 */
export function simplificar(puntos: PuntoTrack[], maximo = MAX_PUNTOS_RUTA): PuntoTrack[] {
  const limpios = puntos.filter((p, i) => i === 0 || p.latitud !== puntos[i - 1].latitud || p.longitud !== puntos[i - 1].longitud);
  if (limpios.length <= maximo) return limpios.map(redondear);

  let bajo = 0;
  let alto = 5000; // metros
  let mejor = rdp(limpios, alto);
  for (let i = 0; i < 25; i++) {
    const medio = (bajo + alto) / 2;
    const resultado = rdp(limpios, medio);
    if (resultado.length <= maximo) {
      mejor = resultado;
      alto = medio;
    } else {
      bajo = medio;
    }
  }
  return mejor.map(redondear);
}

/** Ramer-Douglas-Peucker iterativo (sin recursión, para tracks de decenas de miles de puntos). */
function rdp(puntos: PuntoTrack[], toleranciaMetros: number): PuntoTrack[] {
  const conservar = new Uint8Array(puntos.length);
  conservar[0] = 1;
  conservar[puntos.length - 1] = 1;
  const pila: [number, number][] = [[0, puntos.length - 1]];
  while (pila.length > 0) {
    const [inicio, fin] = pila.pop()!;
    let maxDist = -1;
    let indice = -1;
    for (let i = inicio + 1; i < fin; i++) {
      const d = distanciaASegmento(puntos[i], puntos[inicio], puntos[fin]);
      if (d > maxDist) {
        maxDist = d;
        indice = i;
      }
    }
    if (indice !== -1 && maxDist > toleranciaMetros) {
      conservar[indice] = 1;
      pila.push([inicio, indice], [indice, fin]);
    }
  }
  return puntos.filter((_, i) => conservar[i] === 1);
}

/** Distancia en metros de un punto a un segmento, en proyección local (válida a escala de ruta). */
function distanciaASegmento(p: PuntoTrack, a: PuntoTrack, b: PuntoTrack): number {
  const k = 111_320;
  const cos = Math.cos((a.latitud * Math.PI) / 180);
  const ax = a.longitud * k * cos, ay = a.latitud * k;
  const bx = b.longitud * k * cos, by = b.latitud * k;
  const px = p.longitud * k * cos, py = p.latitud * k;
  const dx = bx - ax, dy = by - ay;
  const largo = dx * dx + dy * dy;
  const t = largo === 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / largo));
  return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
}

/** El backend admite como máximo 7 decimales; 6 dan precisión de 10 cm. */
function redondear(p: PuntoTrack): PuntoTrack {
  return { latitud: Math.round(p.latitud * 1e6) / 1e6, longitud: Math.round(p.longitud * 1e6) / 1e6 };
}

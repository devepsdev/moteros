import { Signal, computed } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, map, of, switchMap } from 'rxjs';
import { TrazadoPreview } from '../models/api.model';
import { RutaService } from '../services/ruta';

/** Límites de la API: con más puntos se trata de un track importado, que ya sigue la carretera. */
const MIN_PUNTOS = 2;
const MAX_PUNTOS = 100;

type Punto = { latitud: number; longitud: number };

const claveDe = (puntos: Punto[]) => puntos.map((p) => `${p.latitud},${p.longitud}`).join(';');

/**
 * Recorrido por carretera de unos puntos, pedido a la API un momento después del último
 * cambio. Mientras no llega el de los puntos actuales (o si no hay), devuelve null y el mapa
 * los une en recto. Se llama en el constructor o al declarar campos (contexto de inyección).
 */
export function trazadoPorCarretera(puntos: Signal<Punto[]>, rutas: RutaService): Signal<TrazadoPreview | null> {
  const respuesta = toSignal(
    toObservable(puntos).pipe(
      debounceTime(600),
      switchMap((lista) => {
        if (lista.length < MIN_PUNTOS || lista.length > MAX_PUNTOS) return of(null);
        const clave = claveDe(lista);
        return rutas.trazado(lista).pipe(
          map((datos) => ({ clave, datos })),
          catchError(() => of(null)),
        );
      }),
    ),
    { initialValue: null },
  );
  return computed(() => {
    const r = respuesta();
    return r && r.clave === claveDe(puntos()) && r.datos.trazado ? r.datos : null;
  });
}

/** Decodifica una polilínea (algoritmo de Google, precisión 5). */
export function decodificarPolilinea(texto: string): [number, number][] {
  const puntos: [number, number][] = [];
  let indice = 0;
  let lat = 0;
  let lon = 0;
  const leer = () => {
    let resultado = 0;
    let desplazamiento = 0;
    let b: number;
    do {
      b = texto.charCodeAt(indice++) - 63;
      resultado |= (b & 0x1f) << desplazamiento;
      desplazamiento += 5;
    } while (b >= 0x20);
    return resultado & 1 ? ~(resultado >> 1) : resultado >> 1;
  };
  while (indice < texto.length) {
    lat += leer();
    lon += leer();
    puntos.push([lat / 1e5, lon / 1e5]);
  }
  return puntos;
}

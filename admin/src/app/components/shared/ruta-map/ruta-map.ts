import { AfterViewInit, Component, ElementRef, OnDestroy, effect, input, output, viewChild } from '@angular/core';
import * as L from 'leaflet';
import { decodificarPolilinea } from '../../../core/trazado';

export interface PuntoMapa {
  latitud: number;
  longitud: number;
  nombre?: string | null;
  /** Punto de paso que fija la carretera elegida: no se dibuja ni se numera. */
  via?: boolean;
}

const NARANJA = '#FF6A13';
const MAX_MARCADORES = 40;
const VISTA_ESPANA: L.LatLngTuple = [40.2, -3.7];

/** Marcador redondo con número de orden (evita las imágenes de icono de Leaflet, que no se empaquetan). */
function icono(texto: string, color: string): L.DivIcon {
  return L.divIcon({
    className: '',
    html: `<div style="width:24px;height:24px;border-radius:12px;background:${color};color:#fff;border:2px solid #0E0F11;
      display:flex;align-items:center;justify-content:center;font:600 11px Inter,sans-serif">${texto}</div>`,
    iconSize: [24, 24],
    iconAnchor: [12, 12],
  });
}

/**
 * Mapa de OpenStreetMap con el recorrido de una ruta. Con `editable`, cada clic añade un punto
 * y los marcadores se pueden arrastrar; los cambios salen por `puntosChange`.
 */
@Component({
  selector: 'app-ruta-map',
  template: `<div #contenedor class="h-full w-full"></div>`,
  host: { class: 'block overflow-hidden rounded-2xl border border-line' },
})
export class RutaMap implements AfterViewInit, OnDestroy {
  readonly puntos = input<PuntoMapa[]>([]);
  readonly editable = input(false);
  /** Recorrido por carretera (polilínea codificada); sin él, la línea une los puntos en recto. */
  readonly trazado = input<string | null | undefined>(null);
  /** Carreteras posibles para un tramo: la elegida resaltada y el resto en gris, clicables. */
  readonly alternativas = input<{ trazado: string; elegida: boolean }[]>([]);
  readonly elegirAlternativa = output<number>();
  readonly puntosChange = output<PuntoMapa[]>();

  private readonly contenedor = viewChild.required<ElementRef<HTMLDivElement>>('contenedor');
  private mapa: L.Map | null = null;
  private capa = L.layerGroup();
  /** Puntos en el último pintado: un salto de más de uno es una carga (ruta, sugerencia, GPX), no un clic. */
  private ultimoTotal = 0;

  constructor() {
    effect(() => {
      const puntos = this.puntos();
      this.trazado();
      this.alternativas();
      if (this.mapa) this.pintar(puntos);
    });
  }

  ngAfterViewInit(): void {
    this.mapa = L.map(this.contenedor().nativeElement, { zoomControl: true }).setView(VISTA_ESPANA, 6);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(this.mapa);
    this.capa.addTo(this.mapa);

    this.mapa.on('click', (e: L.LeafletMouseEvent) => {
      if (!this.editable()) return;
      this.puntosChange.emit([...this.puntos(), { latitud: redondear(e.latlng.lat), longitud: redondear(e.latlng.lng) }]);
    });

    this.pintar(this.puntos());
    // El contenedor puede no tener su tamaño final al crearse (layout en rejilla).
    setTimeout(() => this.mapa?.invalidateSize(), 0);
  }

  ngOnDestroy(): void {
    this.mapa?.remove();
    this.mapa = null;
  }

  private pintar(puntos: PuntoMapa[]): void {
    if (!this.mapa) return;
    this.capa.clearLayers();
    const coords = puntos.map((p) => [p.latitud, p.longitud] as L.LatLngTuple);

    const trazado = this.trazado();
    const linea = trazado ? (decodificarPolilinea(trazado) as L.LatLngTuple[]) : coords;
    const alternativas = this.alternativas();
    alternativas.forEach((a, i) => {
      if (a.elegida) return;
      // Sin propagar el clic al mapa, que añadiría un punto.
      L.polyline(decodificarPolilinea(a.trazado) as L.LatLngTuple[], { color: '#9A9A9A', weight: 6, opacity: 0.85, bubblingMouseEvents: false })
        .on('click', () => this.elegirAlternativa.emit(i))
        .bindTooltip('Ir por esta carretera', { sticky: true })
        .addTo(this.capa);
    });
    if (linea.length > 1) {
      L.polyline(linea, { color: NARANJA, weight: 5, opacity: 0.9 }).addTo(this.capa);
    }
    alternativas.forEach((a) => {
      if (a.elegida) L.polyline(decodificarPolilinea(a.trazado) as L.LatLngTuple[], { color: NARANJA, weight: 6, opacity: 1 }).addTo(this.capa);
    });
    // Con un track importado (cientos de puntos) solo se marcan salida y llegada: los números
    // taparían el recorrido. Con pocos puntos se ven todos y se pueden arrastrar.
    const marcados = puntos.flatMap((p, i) => (p.via ? [] : [i]));
    const detallado = marcados.length <= MAX_MARCADORES;
    marcados.forEach((i, n) => {
      const p = puntos[i];
      const esExtremo = n === 0 || n === marcados.length - 1;
      if (!detallado && !esExtremo) return;
      const color = n === 0 ? '#5FBF8A' : n === marcados.length - 1 && marcados.length > 1 ? '#F0625A' : NARANJA;
      const marcador = L.marker([p.latitud, p.longitud], {
        icon: icono(String(n + 1), color),
        draggable: this.editable() && detallado,
        zIndexOffset: esExtremo ? 1000 : 0,
      });
      if (p.nombre) marcador.bindTooltip(p.nombre);
      if (this.editable()) {
        marcador.on('dragend', () => {
          const { lat, lng } = marcador.getLatLng();
          this.puntosChange.emit(this.puntos().map((q, j) => (j === i ? { ...q, latitud: redondear(lat), longitud: redondear(lng) } : q)));
        });
      }
      marcador.addTo(this.capa);
    });

    // En edición no se mueve la vista con cada clic o «Deshacer», solo cuando se carga un recorrido.
    // Se cuentan solo los puntos marcados: elegir una carretera añade puntos de paso, no es una carga.
    const esCarga = Math.abs(marcados.length - this.ultimoTotal) > 1;
    this.ultimoTotal = marcados.length;
    if (coords.length > 0 && (!this.editable() || esCarga)) {
      if (coords.length === 1) this.mapa.setView(coords[0], 12);
      else this.mapa.fitBounds(L.latLngBounds(linea.length > 1 ? linea : coords), { padding: [32, 32] });
    }
  }
}

/** El backend admite como máximo 7 decimales en las coordenadas. */
function redondear(grados: number): number {
  return Math.round(grados * 1e6) / 1e6;
}

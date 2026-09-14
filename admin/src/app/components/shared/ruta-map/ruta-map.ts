import { AfterViewInit, Component, ElementRef, OnDestroy, effect, input, output, viewChild } from '@angular/core';
import * as L from 'leaflet';

export interface PuntoMapa {
  latitud: number;
  longitud: number;
  nombre?: string | null;
}

const NARANJA = '#FF6A13';
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
  readonly puntosChange = output<PuntoMapa[]>();

  private readonly contenedor = viewChild.required<ElementRef<HTMLDivElement>>('contenedor');
  private mapa: L.Map | null = null;
  private capa = L.layerGroup();
  /** Número de puntos con el que se encuadró por última vez: solo se reencuadra al cargar otra ruta. */
  private encuadrados = -1;

  constructor() {
    effect(() => {
      const puntos = this.puntos();
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

    if (coords.length > 1) {
      L.polyline(coords, { color: NARANJA, weight: 5, opacity: 0.9 }).addTo(this.capa);
    }
    puntos.forEach((p, i) => {
      const esExtremo = i === 0 || i === puntos.length - 1;
      const color = i === 0 ? '#5FBF8A' : i === puntos.length - 1 && puntos.length > 1 ? '#F0625A' : NARANJA;
      const marcador = L.marker([p.latitud, p.longitud], {
        icon: icono(String(i + 1), color),
        draggable: this.editable(),
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

    // En edición no se mueve la vista con cada clic; solo al cargar puntos por primera vez.
    const debeEncuadrar = !this.editable() || this.encuadrados <= 0;
    if (coords.length > 0 && debeEncuadrar && this.encuadrados !== coords.length) {
      if (coords.length === 1) this.mapa.setView(coords[0], 12);
      else this.mapa.fitBounds(L.latLngBounds(coords), { padding: [32, 32] });
      this.encuadrados = coords.length;
    }
  }
}

/** El backend admite como máximo 7 decimales en las coordenadas. */
function redondear(grados: number): number {
  return Math.round(grados * 1e6) / 1e6;
}

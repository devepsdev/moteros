import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { trazadoPorCarretera } from '../../../core/trazado';
import { ESTADO_SUGERENCIA_LABELS, etiquetaDificultad, etiquetaTerreno, formatDuracion, formatKm } from '../../../core/labels';
import { EstadoSugerencia, PageResponse, SugerenciaRuta } from '../../../models/api.model';
import { toApiProblem } from '../../../services/api-error';
import { NotifyService } from '../../../services/notify';
import { RutaService } from '../../../services/ruta';
import { StatsService } from '../../../services/stats';
import { SugerenciaService } from '../../../services/sugerencia';
import { Pagination } from '../../shared/pagination/pagination';
import { PuntoMapa, RutaMap } from '../../shared/ruta-map/ruta-map';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-sugerencia-list',
  imports: [RouterLink, DatePipe, Pagination, RutaMap],
  templateUrl: './sugerencia-list.html',
})
export class SugerenciaList {
  private sugerencias = inject(SugerenciaService);
  private notify = inject(NotifyService);
  private stats = inject(StatsService);

  protected readonly tabs: { value: EstadoSugerencia | null; label: string }[] = [
    { value: 'pendiente', label: 'Pendientes' },
    { value: 'aprobada', label: 'Aprobadas' },
    { value: 'rechazada', label: 'Rechazadas' },
    { value: null, label: 'Todas' },
  ];
  protected readonly estadoLabels = ESTADO_SUGERENCIA_LABELS;
  protected readonly etiquetaDificultad = etiquetaDificultad;
  protected readonly etiquetaTerreno = etiquetaTerreno;
  protected readonly formatKm = formatKm;
  protected readonly formatDuracion = formatDuracion;

  protected readonly estado = signal<EstadoSugerencia | null>('pendiente');
  protected readonly result = signal<PageResponse<SugerenciaRuta> | null>(null);
  protected readonly loading = signal(true);
  protected readonly selected = signal<SugerenciaRuta | null>(null);
  protected readonly rejecting = signal(false);
  protected readonly rejectReason = signal('');
  protected readonly busy = signal(false);
  private page = 0;

  /** Lugares geolocalizados de la sugerencia seleccionada, para la vista previa del recorrido. */
  protected readonly puntosMapa = computed<PuntoMapa[]>(() =>
    (this.selected()?.puntos ?? [])
      .filter((p) => p.latitud != null && p.longitud != null)
      .map((p) => ({ latitud: p.latitud!, longitud: p.longitud!, nombre: p.nombre })),
  );
  /** Cómo quedaría el recorrido por carretera uniendo esos lugares. */
  protected readonly trazadoMapa = trazadoPorCarretera(this.puntosMapa, inject(RutaService));

  constructor() {
    this.load();
  }

  protected setEstado(estado: EstadoSugerencia | null): void {
    this.estado.set(estado);
    this.page = 0;
    this.selected.set(null);
    this.load();
  }

  protected goToPage(page: number): void {
    this.page = page;
    this.load();
  }

  protected select(sugerencia: SugerenciaRuta): void {
    this.selected.set(sugerencia);
    this.rejecting.set(false);
    this.rejectReason.set('');
  }

  protected reject(): void {
    const sugerencia = this.selected();
    if (!sugerencia) return;
    this.busy.set(true);
    this.sugerencias.reject(sugerencia.uuid, this.rejectReason().trim()).subscribe({
      next: () => {
        this.notify.success(`Sugerencia «${sugerencia.nombre}» rechazada.`);
        this.busy.set(false);
        this.selected.set(null);
        this.stats.refresh();
        this.load();
      },
      error: (cause: unknown) => {
        this.notify.error(toApiProblem(cause).message);
        this.busy.set(false);
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.sugerencias.list(this.estado(), this.page, PAGE_SIZE).subscribe({
      next: (page) => {
        this.result.set(page);
        this.loading.set(false);
      },
      error: (cause: unknown) => {
        this.notify.error(toApiProblem(cause).message);
        this.loading.set(false);
      },
    });
  }
}
